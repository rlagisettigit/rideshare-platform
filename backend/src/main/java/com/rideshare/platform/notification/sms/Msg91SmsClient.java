package com.rideshare.platform.notification.sms;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * FR: Section 12 Notification - SMS channel, backed by the MSG91 API
 * (https://docs.msg91.com/reference/send-otp, https://docs.msg91.com/reference/send-sms-flow).
 *
 * Gated by two independent switches so this never becomes a hard dependency, same philosophy
 * as {@link com.rideshare.platform.notification.email.ResendEmailClient}:
 * {@code SMS_NOTIFICATIONS_ENABLED} (a feature toggle, on by default) and the presence of
 * {@code MSG91_AUTH_KEY} (unset in an environment with no MSG91 account). Either being off/blank
 * just logs and no-ops.
 *
 * OTP delivery uses MSG91's dedicated OTP API - {@code MSG91_OTP_TEMPLATE_ID} is optional there;
 * left blank, MSG91 falls back to the account's default OTP template. Every other transactional
 * alert (booking/ride/payment) goes through the templated Flow API, which - per India's TRAI DLT
 * rules - requires a pre-approved template id. {@code MSG91_SMS_TEMPLATE_ID} should point at one
 * generic DLT template registered with a single free-text variable (sent below as {@code var}),
 * so one template covers every alert kind instead of needing one per event type.
 */
@Service
public class Msg91SmsClient {

    private static final Logger log = LoggerFactory.getLogger(Msg91SmsClient.class);

    private final RestClient restClient = RestClient.builder().baseUrl("https://control.msg91.com/api/v5").build();

    private final boolean enabled;
    private final String authKey;
    private final String otpTemplateId;
    private final String smsTemplateId;

    public Msg91SmsClient(
            @Value("${SMS_NOTIFICATIONS_ENABLED:true}") boolean enabled,
            @Value("${MSG91_AUTH_KEY:}") String authKey,
            @Value("${MSG91_OTP_TEMPLATE_ID:}") String otpTemplateId,
            @Value("${MSG91_SMS_TEMPLATE_ID:}") String smsTemplateId) {
        this.enabled = enabled;
        this.authKey = authKey;
        this.otpTemplateId = otpTemplateId;
        this.smsTemplateId = smsTemplateId;
        if (enabled && authKey.isBlank()) {
            log.warn("SMS notifications are enabled but MSG91_AUTH_KEY is not set - SMS will be skipped.");
        }
    }

    /** Delivers a one-time-passcode we generated and hold ourselves (see OtpService) via
     *  MSG91's OTP API - MSG91 only carries it to the handset, it never generates or verifies it. */
    public void sendOtp(String mobile, String otp) {
        if (!ready(mobile)) return;
        try {
            restClient.post()
                    .uri(uriBuilder -> {
                        var b = uriBuilder.path("/otp")
                                .queryParam("mobile", normalize(mobile))
                                .queryParam("otp", otp)
                                .queryParam("authkey", authKey);
                        if (!otpTemplateId.isBlank()) {
                            b.queryParam("template_id", otpTemplateId);
                        }
                        return b.build();
                    })
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send OTP SMS to {}: {}", mobile, e.getMessage());
        }
    }

    /** Sends a transactional alert (booking/ride/payment) as free text through the templated
     *  Flow API - see class javadoc for the single-variable-template assumption. */
    public void sendSms(String mobile, String message) {
        if (!ready(mobile)) return;
        if (smsTemplateId.isBlank()) {
            log.debug("MSG91_SMS_TEMPLATE_ID not set - skipping SMS to {}", mobile);
            return;
        }
        try {
            restClient.post()
                    .uri("/flow")
                    .header("authkey", authKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new FlowRequest(smsTemplateId, "0",
                            List.of(Map.of("mobiles", normalize(mobile), "var", message))))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send SMS to {}: {}", mobile, e.getMessage());
        }
    }

    private boolean ready(String mobile) {
        if (!enabled) {
            log.debug("SMS notifications disabled (SMS_NOTIFICATIONS_ENABLED=false) - skipping SMS to {}", mobile);
            return false;
        }
        return !authKey.isBlank() && mobile != null && !mobile.isBlank();
    }

    /** MSG91 expects a country-code-prefixed number; assumes India (91) when the stored number
     *  is a bare 10-digit local number, matching this platform's other India-specific defaults
     *  (INR currency, DLT-governed SMS templates). */
    private String normalize(String mobile) {
        String digits = mobile.replaceAll("[^0-9]", "");
        return digits.length() == 10 ? "91" + digits : digits;
    }

    private record FlowRequest(
            @JsonProperty("template_id") String templateId,
            @JsonProperty("short_url") String shortUrl,
            List<Map<String, String>> recipients) {}
}

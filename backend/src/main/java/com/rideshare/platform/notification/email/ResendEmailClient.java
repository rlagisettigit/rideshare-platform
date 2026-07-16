package com.rideshare.platform.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * FR: Section 12 Notification - Email channel, backed by the Resend API
 * (https://resend.com/docs/api-reference/emails/send-email).
 *
 * Gated by two independent switches so this never becomes a hard dependency:
 * {@code EMAIL_NOTIFICATIONS_ENABLED} (a feature toggle, on by default) and the presence of
 * {@code RESEND_API_KEY} (unset in an environment with no Resend account). Either being
 * off/blank just logs and no-ops - matches how RoutePreviewService treats a missing
 * MAPPLS_API_KEY as "best-effort, don't break the request".
 */
@Service
public class ResendEmailClient {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailClient.class);

    private final RestClient restClient = RestClient.builder().baseUrl("https://api.resend.com").build();

    private final boolean enabled;
    private final String apiKey;
    private final String fromAddress;

    public ResendEmailClient(
            @Value("${EMAIL_NOTIFICATIONS_ENABLED:true}") boolean enabled,
            @Value("${RESEND_API_KEY:}") String apiKey,
            @Value("${RESEND_FROM_EMAIL:Aura Ride <onboarding@resend.dev>}") String fromAddress) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        if (enabled && apiKey.isBlank()) {
            log.warn("Email notifications are enabled but RESEND_API_KEY is not set - emails will be skipped.");
        }
    }

    public void send(String to, String subject, String html) {
        if (!enabled) {
            log.debug("Email notifications disabled (EMAIL_NOTIFICATIONS_ENABLED=false) - skipping '{}' to {}", subject, to);
            return;
        }
        if (apiKey.isBlank() || to == null || to.isBlank()) {
            return;
        }
        try {
            restClient.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SendRequest(fromAddress, List.of(to), subject, html))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
        }
    }

    private record SendRequest(String from, List<String> to, String subject, String html) {}
}

package com.rideshare.platform.auth.service;

import com.rideshare.platform.auth.entity.OtpCode;
import com.rideshare.platform.auth.repository.OtpCodeRepository;
import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.notification.sms.Msg91SmsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * FR-002 Login (Mobile + OTP), FR-001 post-registration mobile verification. The OTP itself is
 * generated and verified here (hashed at rest, single-use, time-boxed) - {@link Msg91SmsClient}
 * is only the delivery channel, so a slow/failing MSG91 call never affects OTP correctness.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final Msg91SmsClient smsClient;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_VALID_MINUTES = 5;

    public void issueOtp(String mobile, String purpose) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        OtpCode otp = new OtpCode();
        otp.setMobile(mobile);
        otp.setPurpose(purpose);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES));
        otpCodeRepository.save(otp);

        smsClient.sendOtp(mobile, code);
    }

    public void verifyOtp(String mobile, String purpose, String code) {
        OtpCode otp = otpCodeRepository
                .findTopByMobileAndPurposeAndConsumedFalseOrderByCreatedAtDesc(mobile, purpose)
                .orElseThrow(() -> ApiException.badRequest("AUTH_010", "OTP not found or already used."));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("AUTH_011", "OTP has expired.");
        }
        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            throw ApiException.badRequest("AUTH_012", "Invalid OTP.");
        }
        otp.setConsumed(true);
        otpCodeRepository.save(otp);
    }
}

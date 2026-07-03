package com.rideshare.platform.auth.dto;

/** FR-002 Login: supports EMAIL_PASSWORD, MOBILE_OTP, GOOGLE, APPLE. */
public record LoginRequest(
        String mode,          // EMAIL_PASSWORD | MOBILE_OTP | GOOGLE | APPLE
        String email,
        String password,
        String mobile,
        String otp,
        String idToken         // used for GOOGLE / APPLE
) {}

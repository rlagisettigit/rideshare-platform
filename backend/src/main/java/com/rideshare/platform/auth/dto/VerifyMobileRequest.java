package com.rideshare.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** FR-001 post-registration mobile verification: confirms the OTP MSG91 texted to `mobile`. */
public record VerifyMobileRequest(@NotBlank String mobile, @NotBlank String otp) {}

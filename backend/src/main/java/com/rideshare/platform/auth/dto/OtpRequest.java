package com.rideshare.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpRequest(@NotBlank String mobile, String purpose) {}

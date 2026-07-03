package com.rideshare.platform.driver.dto;

import jakarta.validation.constraints.NotBlank;

/** FR: Section 4 Driver Registration - required KYC document references (pre-uploaded to object storage). */
public record DriverOnboardRequest(
        @NotBlank String licenseNumber,
        @NotBlank String licenseDocUrl,
        @NotBlank String governmentIdType,
        @NotBlank String governmentIdDocUrl,
        @NotBlank String addressProofDocUrl,
        @NotBlank String selfieDocUrl
) {}

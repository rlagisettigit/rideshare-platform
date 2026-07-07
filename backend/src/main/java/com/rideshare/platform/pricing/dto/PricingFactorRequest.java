package com.rideshare.platform.pricing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PricingFactorRequest(
        @NotBlank String calculator,
        String vehicleCategory,
        @NotBlank String factorKey,
        @NotNull BigDecimal factorValue,
        String valueType,
        BigDecimal rangeStart,
        BigDecimal rangeEnd,
        boolean active,
        String description
) {}

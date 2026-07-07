package com.rideshare.platform.pricing.dto;

import com.rideshare.platform.pricing.entity.PricingFactor;

import java.math.BigDecimal;

public record PricingFactorResponse(
        Long id,
        String calculator,
        String vehicleCategory,
        String factorKey,
        BigDecimal factorValue,
        String valueType,
        BigDecimal rangeStart,
        BigDecimal rangeEnd,
        boolean active,
        String description
) {
    public static PricingFactorResponse from(PricingFactor f) {
        return new PricingFactorResponse(f.getId(), f.getCalculator(), f.getVehicleCategory(), f.getFactorKey(),
                f.getFactorValue(), f.getValueType(), f.getRangeStart(), f.getRangeEnd(), f.isActive(), f.getDescription());
    }
}

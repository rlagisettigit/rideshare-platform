package com.rideshare.platform.pricing.service;

import com.rideshare.platform.pricing.entity.PricingFactor;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Read-only view over one calculator's active {@link PricingFactor} rows, resolving lookups
 * with "category-specific row wins over the global (vehicle_category NULL) row" semantics.
 */
public class FactorSet {

    private final List<PricingFactor> factors;

    public FactorSet(List<PricingFactor> factors) {
        this.factors = factors;
    }

    /** Exact factor_key match (flat rate, percent, or a named band like a weather condition). */
    public BigDecimal value(String vehicleCategory, String factorKey, BigDecimal defaultValue) {
        return factors.stream()
                .filter(f -> f.getFactorKey().equalsIgnoreCase(factorKey))
                .filter(f -> matchesCategory(f, vehicleCategory))
                .max(Comparator.comparing(f -> f.getVehicleCategory() != null))
                .map(PricingFactor::getFactorValue)
                .orElse(defaultValue);
    }

    /** Row whose [range_start, range_end) window contains the given value, category-specific row wins. */
    public Optional<PricingFactor> matchRange(String vehicleCategory, BigDecimal value) {
        return factors.stream()
                .filter(f -> f.getRangeStart() != null && f.getRangeEnd() != null)
                .filter(f -> matchesCategory(f, vehicleCategory))
                .filter(f -> value.compareTo(f.getRangeStart()) >= 0 && value.compareTo(f.getRangeEnd()) < 0)
                .max(Comparator.comparing(f -> f.getVehicleCategory() != null));
    }

    private boolean matchesCategory(PricingFactor f, String vehicleCategory) {
        return f.getVehicleCategory() == null
                || (vehicleCategory != null && f.getVehicleCategory().equalsIgnoreCase(vehicleCategory));
    }
}

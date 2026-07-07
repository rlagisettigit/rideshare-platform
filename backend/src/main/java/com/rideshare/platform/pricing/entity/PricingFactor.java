package com.rideshare.platform.pricing.entity;

import com.rideshare.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A single configurable fare factor consumed by one FareCalculator. Rows are matched by
 * {@code calculator} + either an exact {@code factorKey} (flat rate, percent, or a named band
 * such as a weather condition) or a numeric [{@code rangeStart}, {@code rangeEnd}) window
 * (peak-hour minute-of-day, demand ratio band, toll distance band) - see FactorSet.
 */
@Getter
@Setter
@Entity
@Table(name = "pricing_factors")
public class PricingFactor extends BaseEntity {

    @Column(nullable = false, length = 40)
    private String calculator;

    @Column(name = "vehicle_category", length = 30)
    private String vehicleCategory;

    @Column(name = "factor_key", nullable = false, length = 60)
    private String factorKey;

    @Column(name = "factor_value", nullable = false)
    private BigDecimal factorValue;

    @Column(name = "value_type", nullable = false, length = 12)
    private String valueType = "FLAT";

    @Column(name = "range_start")
    private BigDecimal rangeStart;

    @Column(name = "range_end")
    private BigDecimal rangeEnd;

    @Column(nullable = false)
    private boolean active = true;

    private String description;
}

package com.rideshare.platform.pricing.service.calculator;

import com.rideshare.platform.pricing.repository.PricingFactorRepository;
import com.rideshare.platform.pricing.service.FactorSet;
import com.rideshare.platform.pricing.service.FareBreakdown;
import com.rideshare.platform.pricing.service.FareCalculator;
import com.rideshare.platform.pricing.service.FareContext;
import com.rideshare.platform.pricing.service.PricingCalculators;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Vehicle category comfort/cost multiplier (factor_key = MULTIPLIER) applied to the ride cost
 * accumulated so far. This is the last calculator building the per-seat ride cost; after it
 * runs, {@code breakdown.runningAmount()} is the finished per-seat cost SeatPricingCalculator reads.
 */
@Component
@Order(9)
@RequiredArgsConstructor
public class VehicleMultiplierCalculator implements FareCalculator {

    private final PricingFactorRepository pricingFactorRepository;

    @Override
    public void apply(FareContext context, FareBreakdown breakdown) {
        FactorSet factorSet = new FactorSet(pricingFactorRepository.findByCalculatorAndActiveTrue(PricingCalculators.VEHICLE_MULTIPLIER));
        BigDecimal multiplier = factorSet.value(context.vehicleCategory(), "MULTIPLIER", BigDecimal.ONE);
        BigDecimal delta = breakdown.runningAmount().multiply(multiplier.subtract(BigDecimal.ONE));
        breakdown.addLine(PricingCalculators.VEHICLE_MULTIPLIER, "Vehicle type adjustment", delta);
    }
}

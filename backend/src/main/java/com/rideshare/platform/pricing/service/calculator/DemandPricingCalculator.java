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
 * Surge multiplier banded by {@link FareContext#demandRatio()} (0-100), a proxy for real-time
 * demand: at booking time this is how full the ride already is; standalone estimates default
 * to 0 (no signal) unless the caller supplies one.
 */
@Component
@Order(6)
@RequiredArgsConstructor
public class DemandPricingCalculator implements FareCalculator {

    private final PricingFactorRepository pricingFactorRepository;

    @Override
    public void apply(FareContext context, FareBreakdown breakdown) {
        FactorSet factorSet = new FactorSet(pricingFactorRepository.findByCalculatorAndActiveTrue(PricingCalculators.DEMAND));
        BigDecimal multiplier = factorSet.matchRange(context.vehicleCategory(), context.demandRatio())
                .map(f -> f.getFactorValue())
                .orElse(BigDecimal.ONE);
        BigDecimal surcharge = breakdown.runningAmount().multiply(multiplier.subtract(BigDecimal.ONE));
        breakdown.addLine(PricingCalculators.DEMAND, "Demand surcharge", surcharge);
    }
}

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
 * Estimated toll cost banded by trip distance (no live toll-plaza data source in this codebase;
 * range_start/range_end on the DISTANCE_BAND rows are in km).
 */
@Component
@Order(5)
@RequiredArgsConstructor
public class TollCalculator implements FareCalculator {

    private final PricingFactorRepository pricingFactorRepository;

    @Override
    public void apply(FareContext context, FareBreakdown breakdown) {
        FactorSet factorSet = new FactorSet(pricingFactorRepository.findByCalculatorAndActiveTrue(PricingCalculators.TOLL));
        BigDecimal toll = factorSet.matchRange(context.vehicleCategory(), context.distanceKm())
                .map(f -> f.getFactorValue())
                .orElse(BigDecimal.ZERO);
        breakdown.addLine(PricingCalculators.TOLL, "Estimated toll", toll);
    }
}

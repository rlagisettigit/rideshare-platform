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

/** Distance-based fare: rate per km (factor_key = PER_KM_RATE) times the trip distance. */
@Component
@Order(2)
@RequiredArgsConstructor
public class DistanceCalculator implements FareCalculator {

    private final PricingFactorRepository pricingFactorRepository;

    @Override
    public void apply(FareContext context, FareBreakdown breakdown) {
        FactorSet factorSet = new FactorSet(pricingFactorRepository.findByCalculatorAndActiveTrue(PricingCalculators.DISTANCE));
        BigDecimal perKm = factorSet.value(context.vehicleCategory(), "PER_KM_RATE", BigDecimal.valueOf(9));
        BigDecimal amount = perKm.multiply(context.distanceKm());
        breakdown.addLine(PricingCalculators.DISTANCE, "Distance charge", amount);
    }
}

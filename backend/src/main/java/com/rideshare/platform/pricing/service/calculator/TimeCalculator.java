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

/** Time-based fare: rate per minute (factor_key = PER_MINUTE_RATE) times the estimated trip duration. */
@Component
@Order(3)
@RequiredArgsConstructor
public class TimeCalculator implements FareCalculator {

    private final PricingFactorRepository pricingFactorRepository;

    @Override
    public void apply(FareContext context, FareBreakdown breakdown) {
        FactorSet factorSet = new FactorSet(pricingFactorRepository.findByCalculatorAndActiveTrue(PricingCalculators.TIME));
        BigDecimal perMinute = factorSet.value(context.vehicleCategory(), "PER_MINUTE_RATE", BigDecimal.valueOf(1.5));
        BigDecimal amount = perMinute.multiply(context.durationMinutes());
        breakdown.addLine(PricingCalculators.TIME, "Time charge", amount);
    }
}

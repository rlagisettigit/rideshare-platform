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
 * Weather multiplier keyed by {@link FareContext#weatherCondition()} (e.g. CLEAR/RAIN/STORM/FOG).
 * No live weather feed is integrated here - the condition is caller-supplied (or defaults to
 * CLEAR, a 1.0 multiplier).
 */
@Component
@Order(8)
@RequiredArgsConstructor
public class WeatherCalculator implements FareCalculator {

    private final PricingFactorRepository pricingFactorRepository;

    @Override
    public void apply(FareContext context, FareBreakdown breakdown) {
        FactorSet factorSet = new FactorSet(pricingFactorRepository.findByCalculatorAndActiveTrue(PricingCalculators.WEATHER));
        BigDecimal multiplier = factorSet.value(context.vehicleCategory(), context.weatherCondition(), BigDecimal.ONE);
        BigDecimal surcharge = breakdown.runningAmount().multiply(multiplier.subtract(BigDecimal.ONE));
        breakdown.addLine(PricingCalculators.WEATHER, "Weather surcharge (" + context.weatherCondition() + ")", surcharge);
    }
}

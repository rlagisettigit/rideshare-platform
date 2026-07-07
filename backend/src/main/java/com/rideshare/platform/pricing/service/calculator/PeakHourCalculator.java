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

/** Peak-hour multiplier banded by minute-of-day (0-1440) of the ride's pickup/departure time. */
@Component
@Order(7)
@RequiredArgsConstructor
public class PeakHourCalculator implements FareCalculator {

    private final PricingFactorRepository pricingFactorRepository;

    @Override
    public void apply(FareContext context, FareBreakdown breakdown) {
        FactorSet factorSet = new FactorSet(pricingFactorRepository.findByCalculatorAndActiveTrue(PricingCalculators.PEAK_HOUR));
        BigDecimal minuteOfDay = BigDecimal.valueOf(context.minuteOfDay());
        BigDecimal multiplier = factorSet.matchRange(context.vehicleCategory(), minuteOfDay)
                .map(f -> f.getFactorValue())
                .orElse(BigDecimal.ONE);
        BigDecimal surcharge = breakdown.runningAmount().multiply(multiplier.subtract(BigDecimal.ONE));
        breakdown.addLine(PricingCalculators.PEAK_HOUR, "Peak-hour surcharge", surcharge);
    }
}

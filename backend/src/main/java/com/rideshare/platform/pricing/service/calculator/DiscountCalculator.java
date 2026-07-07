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
 * Combines an admin-tunable general promo percent (factor_key = GENERAL_DISCOUNT_PERCENT) with
 * a time-banded off-peak percent (factor_key = OFF_PEAK_DISCOUNT_PERCENT, ranged on minute-of-day),
 * capped at 50% combined so a misconfigured factor can't zero out or invert the fare.
 */
@Component
@Order(11)
@RequiredArgsConstructor
public class DiscountCalculator implements FareCalculator {

    private static final BigDecimal MAX_COMBINED_DISCOUNT_PERCENT = BigDecimal.valueOf(50);

    private final PricingFactorRepository pricingFactorRepository;

    @Override
    public void apply(FareContext context, FareBreakdown breakdown) {
        FactorSet factorSet = new FactorSet(pricingFactorRepository.findByCalculatorAndActiveTrue(PricingCalculators.DISCOUNT));

        BigDecimal generalPercent = factorSet.value(context.vehicleCategory(), "GENERAL_DISCOUNT_PERCENT", BigDecimal.ZERO);
        BigDecimal offPeakPercent = factorSet.matchRange(context.vehicleCategory(), BigDecimal.valueOf(context.minuteOfDay()))
                .map(f -> f.getFactorValue())
                .orElse(BigDecimal.ZERO);

        BigDecimal combinedPercent = generalPercent.add(offPeakPercent).min(MAX_COMBINED_DISCOUNT_PERCENT);
        BigDecimal discount = breakdown.runningAmount().multiply(combinedPercent).divide(BigDecimal.valueOf(100));

        breakdown.setDiscountAmount(discount);
        breakdown.addLine(PricingCalculators.DISCOUNT, "Discount", discount.negate());
        breakdown.setFareAfterDiscount(breakdown.runningAmount());
    }
}

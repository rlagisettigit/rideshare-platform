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
 * Platform commission (factor_key = COMMISSION_PERCENT) taken from the driver's payout on the
 * post-discount, pre-tax fare. Deliberately does NOT change {@code breakdown.runningAmount()} -
 * commission is not charged to the passenger, only deducted from what the driver receives.
 */
@Component
@Order(12)
@RequiredArgsConstructor
public class CommissionCalculator implements FareCalculator {

    private final PricingFactorRepository pricingFactorRepository;

    @Override
    public void apply(FareContext context, FareBreakdown breakdown) {
        FactorSet factorSet = new FactorSet(pricingFactorRepository.findByCalculatorAndActiveTrue(PricingCalculators.COMMISSION));
        BigDecimal percent = factorSet.value(context.vehicleCategory(), "COMMISSION_PERCENT", BigDecimal.valueOf(15));
        BigDecimal commission = breakdown.runningAmount().multiply(percent).divide(BigDecimal.valueOf(100));

        breakdown.setCommissionAmount(commission);
        breakdown.addInfoLine(PricingCalculators.COMMISSION, "Platform commission (from driver payout)", commission);
    }
}

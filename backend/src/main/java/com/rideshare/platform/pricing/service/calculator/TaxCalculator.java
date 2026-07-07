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

/** GST-style tax (factor_key = TAX_PERCENT) on the post-discount fare, added to what the passenger pays. */
@Component
@Order(13)
@RequiredArgsConstructor
public class TaxCalculator implements FareCalculator {

    private final PricingFactorRepository pricingFactorRepository;

    @Override
    public void apply(FareContext context, FareBreakdown breakdown) {
        FactorSet factorSet = new FactorSet(pricingFactorRepository.findByCalculatorAndActiveTrue(PricingCalculators.TAX));
        BigDecimal percent = factorSet.value(context.vehicleCategory(), "TAX_PERCENT", BigDecimal.valueOf(5));
        BigDecimal tax = breakdown.runningAmount().multiply(percent).divide(BigDecimal.valueOf(100));

        breakdown.setTaxAmount(tax);
        breakdown.addLine(PricingCalculators.TAX, "Tax", tax);
    }
}

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

/** Fuel-price surcharge: percent (factor_key = FUEL_SURCHARGE_PERCENT) of the base+distance+time subtotal. */
@Component
@Order(4)
@RequiredArgsConstructor
public class FuelAdjustmentCalculator implements FareCalculator {

    private final PricingFactorRepository pricingFactorRepository;

    @Override
    public void apply(FareContext context, FareBreakdown breakdown) {
        FactorSet factorSet = new FactorSet(pricingFactorRepository.findByCalculatorAndActiveTrue(PricingCalculators.FUEL_ADJUSTMENT));
        BigDecimal percent = factorSet.value(context.vehicleCategory(), "FUEL_SURCHARGE_PERCENT", BigDecimal.valueOf(4));
        BigDecimal amount = breakdown.runningAmount().multiply(percent).divide(BigDecimal.valueOf(100));
        breakdown.addLine(PricingCalculators.FUEL_ADJUSTMENT, "Fuel adjustment", amount);
    }
}

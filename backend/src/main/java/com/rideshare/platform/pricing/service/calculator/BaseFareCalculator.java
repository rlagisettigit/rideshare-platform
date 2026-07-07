package com.rideshare.platform.pricing.service.calculator;

import com.rideshare.platform.pricing.entity.PricingFactor;
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
import java.util.List;

/** Flat starting fare for the trip, keyed by vehicle category (factor_key = BASE_FARE). */
@Component
@Order(1)
@RequiredArgsConstructor
public class BaseFareCalculator implements FareCalculator {

    private final PricingFactorRepository pricingFactorRepository;

    @Override
    public void apply(FareContext context, FareBreakdown breakdown) {
        List<PricingFactor> factors = pricingFactorRepository.findByCalculatorAndActiveTrue(PricingCalculators.BASE_FARE);
        FactorSet factorSet = new FactorSet(factors);
        BigDecimal baseFare = factorSet.value(context.vehicleCategory(), "BASE_FARE", BigDecimal.valueOf(50));
        breakdown.addLine(PricingCalculators.BASE_FARE, "Base fare", baseFare);
    }
}

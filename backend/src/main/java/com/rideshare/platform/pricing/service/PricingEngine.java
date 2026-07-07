package com.rideshare.platform.pricing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Runs every {@link FareCalculator} bean in order (Spring sorts the injected List<FareCalculator>
 * by each bean's {@code @Order}) against a fresh {@link FareBreakdown}, chain-of-responsibility
 * style: each calculator both reads and mutates the shared breakdown.
 */
@Service
@RequiredArgsConstructor
public class PricingEngine {

    private final List<FareCalculator> calculators;

    public FareBreakdown calculate(FareContext context) {
        FareBreakdown breakdown = new FareBreakdown();
        for (FareCalculator calculator : calculators) {
            calculator.apply(context, breakdown);
        }
        return breakdown;
    }
}

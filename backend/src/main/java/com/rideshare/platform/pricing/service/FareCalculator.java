package com.rideshare.platform.pricing.service;

/** One stage in the fare calculation chain. Implementations are ordered via {@code @Order}. */
public interface FareCalculator {
    void apply(FareContext context, FareBreakdown breakdown);
}

package com.rideshare.platform.pricing.service.calculator;

import com.rideshare.platform.pricing.service.FareBreakdown;
import com.rideshare.platform.pricing.service.FareCalculator;
import com.rideshare.platform.pricing.service.FareContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Assembles the final totals from everything the chain has accumulated. Not config-driven -
 * this stage only aggregates what CommissionCalculator and TaxCalculator already computed:
 * passengerFare = fareAfterDiscount + tax; driverPayout = fareAfterDiscount - commission.
 */
@Component
@Order(14)
public class FinalFareCalculator implements FareCalculator {

    @Override
    public void apply(FareContext context, FareBreakdown breakdown) {
        breakdown.setPassengerFare(breakdown.fareAfterDiscount().add(breakdown.taxAmount()));
        breakdown.setDriverPayout(breakdown.fareAfterDiscount().subtract(breakdown.commissionAmount()));
    }
}

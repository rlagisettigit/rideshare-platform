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
import java.math.RoundingMode;

/**
 * Base..VehicleMultiplier price the cost of running the WHOLE vehicle for the trip, not one
 * seat - so this first divides that trip cost by the vehicle's total seating capacity to get
 * the actual per-seat cost (a 1-seat booking in a 4-seat car must be charged 1/4 of the trip,
 * not the whole trip), then charges the seats actually booked: the first seat in full, each
 * additional one at factor_key = ADDITIONAL_SEAT_PERCENT of the per-seat cost (a small pooling
 * discount).
 */
@Component
@Order(10)
@RequiredArgsConstructor
public class SeatPricingCalculator implements FareCalculator {

    private final PricingFactorRepository pricingFactorRepository;

    @Override
    public void apply(FareContext context, FareBreakdown breakdown) {
        BigDecimal totalVehicleTripCost = breakdown.runningAmount();
        BigDecimal perSeatCost = totalVehicleTripCost.divide(BigDecimal.valueOf(context.vehicleTotalSeats()), 6, RoundingMode.HALF_UP);
        breakdown.setRideCostPerSeat(perSeatCost);

        FactorSet factorSet = new FactorSet(pricingFactorRepository.findByCalculatorAndActiveTrue(PricingCalculators.SEAT_PRICING));
        BigDecimal additionalSeatPercent = factorSet.value(context.vehicleCategory(), "ADDITIONAL_SEAT_PERCENT", BigDecimal.valueOf(100));
        int additionalSeats = Math.max(0, context.seats() - 1);

        BigDecimal additionalSeatsCost = perSeatCost
                .multiply(additionalSeatPercent).divide(BigDecimal.valueOf(100))
                .multiply(BigDecimal.valueOf(additionalSeats));
        BigDecimal farePreDiscount = perSeatCost.add(additionalSeatsCost);

        breakdown.setFarePreDiscount(farePreDiscount);
        breakdown.setRunningAmount(farePreDiscount);
        breakdown.addInfoLine(PricingCalculators.SEAT_PRICING,
                "Seat pricing: trip cost " + totalVehicleTripCost + " ÷ " + context.vehicleTotalSeats()
                        + " vehicle seats, " + context.seats() + " booked", farePreDiscount);
    }
}

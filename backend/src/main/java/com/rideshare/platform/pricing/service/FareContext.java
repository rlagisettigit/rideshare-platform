package com.rideshare.platform.pricing.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable inputs to a single PricingEngine.calculate() run. {@code demandRatio} (0-100, percent
 * of the ride's seats already booked) and {@code weatherCondition} have no live external data
 * source in this codebase - callers pass what they know, defaulting to "no signal" values.
 *
 * {@code vehicleTotalSeats} matters: Base..VehicleMultiplier price the cost of running the whole
 * vehicle for the whole trip, not one seat - SeatPricingCalculator divides that trip cost by this
 * to get the actual per-seat cost, then charges {@code seats} of it. A 1-seat booking in a
 * 4-seat car must be charged 1/4 of the trip cost, not the whole trip.
 */
public class FareContext {

    private final String vehicleCategory;
    private final BigDecimal distanceKm;
    private final BigDecimal durationMinutes;
    private final int seats;
    private final int vehicleTotalSeats;
    private final LocalDateTime pickupAt;
    private final BigDecimal demandRatio;
    private final String weatherCondition;

    public FareContext(String vehicleCategory, BigDecimal distanceKm, BigDecimal durationMinutes, int seats,
                        int vehicleTotalSeats, LocalDateTime pickupAt, BigDecimal demandRatio, String weatherCondition) {
        this.vehicleCategory = vehicleCategory;
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
        this.seats = seats;
        this.vehicleTotalSeats = Math.max(1, vehicleTotalSeats);
        this.pickupAt = pickupAt != null ? pickupAt : LocalDateTime.now();
        this.demandRatio = demandRatio != null ? demandRatio : BigDecimal.ZERO;
        this.weatherCondition = weatherCondition != null ? weatherCondition : "CLEAR";
    }

    public String vehicleCategory() { return vehicleCategory; }
    public BigDecimal distanceKm() { return distanceKm; }
    public BigDecimal durationMinutes() { return durationMinutes; }
    /** Seats being booked/quoted in this fare run. */
    public int seats() { return seats; }
    /** Total seating capacity of the vehicle running the trip - the denominator SeatPricingCalculator splits the trip cost across. */
    public int vehicleTotalSeats() { return vehicleTotalSeats; }
    public LocalDateTime pickupAt() { return pickupAt; }
    /** 0-100: percent of the ride's total seats already booked, used as a demand proxy. */
    public BigDecimal demandRatio() { return demandRatio; }
    public String weatherCondition() { return weatherCondition; }
    public int minuteOfDay() { return pickupAt.getHour() * 60 + pickupAt.getMinute(); }
}

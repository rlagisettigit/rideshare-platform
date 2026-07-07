package com.rideshare.platform.search.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FR: Section 8 Ride Search - Search Ranking fields included so client can sort/filter.
 * {@code pricePerSeat} is the driver's advertised listing price; {@code estimatedFare} is what
 * the PricingEngine actually computes for THIS search's pickup/drop/seat count - the number a
 * passenger should be shown, since that's what a booking will actually charge (see
 * {@code fareDisclaimer} for the caveat to display alongside it).
 */
public record RideSearchResult(
        String ridePublicId,
        String driverName,
        Double driverRating,
        String vehicleModel,
        LocalDateTime departureAt,
        int availableSeats,
        BigDecimal pricePerSeat,
        BigDecimal estimatedFare,
        String fareDisclaimer,
        double pickupDistanceKm,
        double detourKm,
        int pickupSequenceNo,
        int dropSequenceNo,
        String recurringRidePublicId
) {}

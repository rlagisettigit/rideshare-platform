package com.rideshare.platform.search.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** FR: Section 8 Ride Search - Search Ranking fields included so client can sort/filter. */
public record RideSearchResult(
        String ridePublicId,
        String driverName,
        Double driverRating,
        String vehicleModel,
        LocalDateTime departureAt,
        int availableSeats,
        BigDecimal pricePerSeat,
        double pickupDistanceKm,
        double detourKm,
        int pickupSequenceNo,
        int dropSequenceNo,
        String recurringRidePublicId
) {}

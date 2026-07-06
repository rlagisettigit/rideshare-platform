package com.rideshare.platform.ride.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Books a passenger onto occurrences of a recurring ride series in one action.
 * If ridePublicIds is null/empty, every upcoming occurrence is booked; otherwise only the
 * specified occurrences are - lets a passenger pick, say, Mon/Wed/Fri out of a Mon-Fri series.
 */
public record RecurringBookingRequest(
        @NotNull Double pickupLat,
        @NotNull Double pickupLng,
        String pickupAddress,
        @NotNull Double dropLat,
        @NotNull Double dropLng,
        String dropAddress,
        @NotNull @Min(1) Integer seats,
        List<String> ridePublicIds
) {}

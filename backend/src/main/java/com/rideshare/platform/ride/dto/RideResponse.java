package com.rideshare.platform.ride.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RideResponse(
        String publicId,
        String driverName,
        Double driverRating,
        String vehicleModel,
        String originAddress,
        String destinationAddress,
        LocalDateTime departureAt,
        Integer availableSeats,
        BigDecimal pricePerSeat,
        boolean womenOnly,
        boolean petsAllowed,
        boolean luggageAllowed,
        String status,
        String recurringRidePublicId
) {}

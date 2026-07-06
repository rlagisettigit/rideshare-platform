package com.rideshare.platform.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        String publicId,
        String ridePublicId,
        String status,
        int seatsBooked,
        BigDecimal fare,
        String pickupAddress,
        double pickupLat,
        double pickupLng,
        String dropAddress,
        double dropLat,
        double dropLng,
        String passengerName,
        String passengerUserPublicId,
        String rideOriginAddress,
        String rideDestinationAddress,
        LocalDateTime rideDepartureAt,
        String rideStatus,
        String bookingBatchId,
        String recurringRidePublicId
) {}

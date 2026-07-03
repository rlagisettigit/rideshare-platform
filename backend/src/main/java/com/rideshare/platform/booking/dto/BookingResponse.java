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
        String dropAddress,
        String passengerName,
        String rideOriginAddress,
        String rideDestinationAddress,
        LocalDateTime rideDepartureAt,
        String rideStatus
) {}

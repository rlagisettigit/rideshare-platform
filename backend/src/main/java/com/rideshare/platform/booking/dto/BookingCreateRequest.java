package com.rideshare.platform.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** FR: Section 9 Booking Management - Passenger requests booking (supports partial-route bookings). */
public record BookingCreateRequest(
        @NotBlank String ridePublicId,
        @NotNull Double pickupLat,
        @NotNull Double pickupLng,
        String pickupAddress,
        @NotNull Double dropLat,
        @NotNull Double dropLng,
        String dropAddress,
        @NotNull @Min(1) Integer seats
) {}

package com.rideshare.platform.ride.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Creates a repeating schedule; generates one independently-bookable Ride per matching date immediately. */
public record RecurringRideCreateRequest(
        @NotNull Long vehicleId,

        @NotBlank String originAddress,
        @NotNull Double originLat,
        @NotNull Double originLng,

        @NotBlank String destinationAddress,
        @NotNull Double destinationLat,
        @NotNull Double destinationLng,

        @NotEmpty List<DayOfWeek> daysOfWeek,
        @NotNull LocalTime departureTime,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,

        @NotNull @Min(1) Integer availableSeats,
        @NotNull @DecimalMin("0.0") BigDecimal pricePerSeat,

        boolean luggageAllowed,
        boolean smokingAllowed,
        boolean womenOnly,
        boolean petsAllowed,
        @Size(max = 500) String description,

        @DecimalMin("0.0") BigDecimal maxDetourKm,

        // Optional: set when the driver picked a specific option from the route preview,
        // same as a one-off ride publish - reused unchanged for every generated occurrence.
        String selectedRouteProvider,
        String selectedRoutePolyline,
        Integer selectedRouteDistanceMeters,
        Integer selectedRouteDurationSeconds
) {}

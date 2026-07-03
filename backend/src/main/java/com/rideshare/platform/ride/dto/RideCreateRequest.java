package com.rideshare.platform.ride.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

/** FR: Section 6 Ride Management - Create Ride. */
public record RideCreateRequest(
        @NotNull Long vehicleId,

        @NotBlank String originAddress,
        @NotNull Double originLat,
        @NotNull Double originLng,

        @NotBlank String destinationAddress,
        @NotNull Double destinationLat,
        @NotNull Double destinationLng,

        @NotNull LocalDate departureDate,
        @NotNull LocalTime departureTime,

        @NotNull @Min(1) Integer availableSeats,
        @NotNull @DecimalMin("0.0") BigDecimal pricePerSeat,

        boolean luggageAllowed,
        boolean smokingAllowed,
        String musicPreference,
        boolean womenOnly,
        boolean petsAllowed,
        @Size(max = 500) String description,

        @DecimalMin("0.0") BigDecimal maxDetourKm,

        // Optional: set when the driver picked a specific option from the route preview
        // (see RouteController#preview) instead of leaving the default route generation.
        String selectedRouteProvider,
        String selectedRoutePolyline,
        Integer selectedRouteDistanceMeters,
        Integer selectedRouteDurationSeconds
) {}

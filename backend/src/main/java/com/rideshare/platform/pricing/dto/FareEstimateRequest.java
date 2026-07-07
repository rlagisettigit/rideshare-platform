package com.rideshare.platform.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Inputs for a standalone fare quote (frontend preview before publishing a ride or booking one).
 * {@code vehicleTotalSeats} is the vehicle's full seating capacity, NOT the seats being quoted -
 * the trip cost is split across it, then {@code seats} of that per-seat cost is charged (so a
 * 1-seat booking in a 4-seat car is priced at roughly 1/4 the trip cost, not the whole trip).
 * {@code durationMinutes}, {@code pickupAt}, {@code demandRatio} and {@code weatherCondition} are
 * all optional - see PricingController for their defaults.
 */
public record FareEstimateRequest(
        @NotBlank String vehicleCategory,
        @NotNull @DecimalMin(value = "0.1", message = "distanceKm must be positive") BigDecimal distanceKm,
        BigDecimal durationMinutes,
        @NotNull @Min(1) Integer seats,
        @NotNull @Min(1) Integer vehicleTotalSeats,
        LocalDateTime pickupAt,
        @DecimalMin("0") BigDecimal demandRatio,
        String weatherCondition
) {}

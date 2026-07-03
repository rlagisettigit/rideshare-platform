package com.rideshare.platform.search.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** FR: Section 8 Ride Search - Passenger inputs. */
public record RideSearchRequest(
        @NotNull Double pickupLat,
        @NotNull Double pickupLng,
        @NotNull Double dropLat,
        @NotNull Double dropLng,
        @NotNull LocalDate travelDate,
        int passengers,
        int page,
        int size,
        SortOption sortBy
) {
    public enum SortOption { NEAREST_PICKUP, LOWEST_DETOUR, EARLIEST_DEPARTURE, DRIVER_RATING, RIDE_PRICE }
}

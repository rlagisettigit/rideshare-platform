package com.rideshare.platform.location.dto;

import jakarta.validation.constraints.NotNull;

/** Sent by the driver's browser every few seconds via navigator.geolocation while a ride is IN_PROGRESS. */
public record LocationUpdateRequest(
        @NotNull Double lat,
        @NotNull Double lng,
        Double heading
) {}

package com.rideshare.platform.route.dto;

import jakarta.validation.constraints.NotNull;

/** FR: Section 6 Ride Management - driver route preview before publishing. */
public record RoutePreviewRequest(
        @NotNull Double originLat,
        @NotNull Double originLng,
        @NotNull Double destinationLat,
        @NotNull Double destinationLng
) {}

package com.rideshare.platform.route.dto;

import java.util.List;

/** A single driver-facing route alternative for the publish-time route preview. */
public record RouteOption(
        String label,
        int distanceMeters,
        int durationSeconds,
        boolean tollFree,
        List<String> majorPlaces,
        String encodedPolyline
) {}

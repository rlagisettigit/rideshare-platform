package com.rideshare.platform.route.dto;

import jakarta.validation.constraints.NotBlank;

/** FR: Section 6 Ride Management - on-demand major-cities lookup for one previewed route option. */
public record RoutePlacesRequest(
        @NotBlank String encodedPolyline
) {}

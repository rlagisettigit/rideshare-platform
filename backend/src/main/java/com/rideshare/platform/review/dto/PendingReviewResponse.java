package com.rideshare.platform.review.dto;

import java.time.LocalDateTime;

/** A completed ride the current user hasn't yet rated the other party for. */
public record PendingReviewResponse(
        String ridePublicId,
        String revieweeUserPublicId,
        String revieweeName,
        String direction, // RATE_DRIVER or RATE_PASSENGER
        String rideOriginAddress,
        String rideDestinationAddress,
        LocalDateTime rideDepartureAt
) {}

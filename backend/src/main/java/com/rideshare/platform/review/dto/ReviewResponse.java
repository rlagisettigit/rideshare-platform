package com.rideshare.platform.review.dto;

import java.time.LocalDateTime;

/** A review the current user has received. */
public record ReviewResponse(
        Long id,
        String reviewerName,
        int rating,
        String comment,
        LocalDateTime createdAt
) {}

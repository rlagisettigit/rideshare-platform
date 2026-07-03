package com.rideshare.platform.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UserProfileResponse(
        String publicId,
        String name,
        String email,
        String mobile,
        String gender,
        LocalDate dob,
        String profilePhotoUrl,
        String preferredLanguage,
        Double homeLat,
        Double homeLng,
        Double officeLat,
        Double officeLng,
        boolean rolePassenger,
        boolean roleDriver,
        BigDecimal averageRating
) {}

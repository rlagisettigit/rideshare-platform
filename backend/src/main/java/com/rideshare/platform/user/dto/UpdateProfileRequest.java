package com.rideshare.platform.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** FR-003 Profile Management - all fields optional (partial update / PATCH semantics). */
public record UpdateProfileRequest(
        @Size(max = 120) String name,
        String profilePhotoUrl,
        String gender,
        LocalDate dob,
        @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid mobile number") String mobile,
        String preferredLanguage,
        Double homeLat,
        Double homeLng,
        Double officeLat,
        Double officeLng
) {}

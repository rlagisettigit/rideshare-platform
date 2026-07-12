package com.rideshare.platform.common;

import com.rideshare.platform.common.exception.ApiException;

import java.time.LocalDate;
import java.time.Period;

/** Shared minimum-age rule for both registration and profile edits (a user can't lower their age below 18 by editing dob after signup). */
public final class AgeValidator {

    private static final int MINIMUM_AGE_YEARS = 18;

    private AgeValidator() {
    }

    public static void requireAtLeast18(LocalDate dob) {
        if (dob != null && Period.between(dob, LocalDate.now()).getYears() < MINIMUM_AGE_YEARS) {
            throw ApiException.badRequest("AUTH_009", "You must be at least 18 years old to use this platform.");
        }
    }
}

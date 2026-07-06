package com.rideshare.platform.ride.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One bookable date within a recurring ride series, for the passenger's day-picker before booking. */
public record RecurringOccurrenceResponse(
        String ridePublicId,
        LocalDate departureDate,
        LocalDateTime departureAt,
        int availableSeats
) {}

package com.rideshare.platform.ride.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record RecurringRideResponse(
        String publicId,
        String originAddress,
        String destinationAddress,
        List<String> daysOfWeek,
        LocalTime departureTime,
        LocalDate startDate,
        LocalDate endDate,
        Integer availableSeats,
        BigDecimal pricePerSeat,
        String status,
        int occurrenceCount,
        int upcomingCount
) {}

package com.rideshare.platform.ride.dto;

import com.rideshare.platform.booking.dto.BookingResponse;

import java.util.List;

public record RecurringBookingSummary(
        int requested,
        int booked,
        int failed,
        List<BookingResponse> bookings,
        List<String> failures
) {}

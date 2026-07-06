package com.rideshare.platform.booking.dto;

import java.util.List;

/** Result of accepting/rejecting every booking in a batch (see Booking.bookingBatchId) at once. */
public record BookingBatchSummary(
        int requested,
        int succeeded,
        int failed,
        List<BookingResponse> bookings,
        List<String> failures
) {}

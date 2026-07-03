package com.rideshare.platform.kafka.events;

public record BookingCancelledEvent(String bookingPublicId, String ridePublicId, String cancelledBy, String reason) {}

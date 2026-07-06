package com.rideshare.platform.kafka.events;

public record BookingRejectedEvent(String bookingPublicId, String ridePublicId, String passengerPublicId) {}

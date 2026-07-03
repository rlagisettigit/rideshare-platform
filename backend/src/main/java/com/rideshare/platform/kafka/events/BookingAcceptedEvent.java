package com.rideshare.platform.kafka.events;

public record BookingAcceptedEvent(String bookingPublicId, String ridePublicId, String passengerPublicId) {}

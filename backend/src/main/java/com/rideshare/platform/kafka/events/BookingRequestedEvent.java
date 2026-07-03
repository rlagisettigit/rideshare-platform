package com.rideshare.platform.kafka.events;

public record BookingRequestedEvent(String bookingPublicId, String ridePublicId, String passengerPublicId, int seats) {}

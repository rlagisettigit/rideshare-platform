package com.rideshare.platform.kafka.events;

import java.time.LocalDateTime;

public record RidePublishedEvent(String ridePublicId, String driverPublicId, LocalDateTime departureAt) {}

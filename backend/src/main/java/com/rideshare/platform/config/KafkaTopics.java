package com.rideshare.platform.config;

/** FR: Section 12 Notification Events - canonical Kafka topic names. */
public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String RIDE_PUBLISHED = "ride.published";
    public static final String BOOKING_REQUESTED = "booking.requested";
    public static final String BOOKING_ACCEPTED = "booking.accepted";
    public static final String BOOKING_REJECTED = "booking.rejected";
    public static final String BOOKING_CANCELLED = "booking.cancelled";
    public static final String RIDE_STARTED = "ride.started";
    public static final String RIDE_COMPLETED = "ride.completed";
    public static final String AUDIT_EVENTS = "audit.events";
}

package com.rideshare.platform.notification.listener;

import com.rideshare.platform.booking.entity.Booking;
import com.rideshare.platform.booking.entity.BookingStatus;
import com.rideshare.platform.booking.repository.BookingRepository;
import com.rideshare.platform.config.KafkaTopics;
import com.rideshare.platform.kafka.events.*;
import com.rideshare.platform.notification.service.NotificationDispatchService;
import com.rideshare.platform.ride.entity.Ride;
import com.rideshare.platform.ride.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR: Section 12 Notification Events - Ride Published, Booking Requested/Accepted/Cancelled,
 * Ride Started, Ride Completed. Consumes the domain events published by ride/booking services
 * and fans them out across notification channels (Section 12).
 */
@Component
@RequiredArgsConstructor
public class RideBookingEventListener {

    private final NotificationDispatchService dispatchService;
    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;

    @KafkaListener(topics = KafkaTopics.RIDE_PUBLISHED, groupId = "notification-service")
    public void onRidePublished(RidePublishedEvent event) {
        // TODO: resolve subscribed passengers near the route and notify (Push/In-App)
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_REQUESTED, groupId = "notification-service")
    public void onBookingRequested(BookingRequestedEvent event) {
        // notify the driver that a new booking request needs a response
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_CANCELLED, groupId = "notification-service")
    public void onBookingCancelled(BookingCancelledEvent event) {
        // notify the counterparty of the cancellation
    }

    @KafkaListener(topics = KafkaTopics.RIDE_STARTED, groupId = "notification-service")
    @Transactional(readOnly = true)
    public void onRideStarted(RideStartedEvent event) {
        rideRepository.findByPublicId(event.ridePublicId()).ifPresent(ride ->
                notifyConfirmedPassengers(ride, "RIDE_STARTED", "Your ride has started",
                        "Your driver has started the trip from " + ride.getOriginAddress()
                                + " to " + ride.getDestinationAddress() + "."));
    }

    @KafkaListener(topics = KafkaTopics.RIDE_COMPLETED, groupId = "notification-service")
    @Transactional(readOnly = true)
    public void onRideCompleted(RideCompletedEvent event) {
        rideRepository.findByPublicId(event.ridePublicId()).ifPresent(ride ->
                notifyCompletedPassengers(ride, "RIDE_COMPLETED", "Your ride is complete",
                        "Your trip from " + ride.getOriginAddress() + " to " + ride.getDestinationAddress()
                                + " has finished. Thanks for riding!"));
    }

    private void notifyConfirmedPassengers(Ride ride, String eventType, String title, String body) {
        for (Booking booking : bookingRepository.findByRideId(ride.getId())) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                dispatchService.send(booking.getPassenger().getId(), "IN_APP", eventType, title, body, ride.getPublicId());
            }
        }
    }

    private void notifyCompletedPassengers(Ride ride, String eventType, String title, String body) {
        for (Booking booking : bookingRepository.findByRideId(ride.getId())) {
            if (booking.getStatus() == BookingStatus.COMPLETED) {
                Long passengerId = booking.getPassenger().getId();
                // The "ride started" notification is no longer relevant once the ride is done.
                dispatchService.removeByReference(passengerId, ride.getPublicId(), "RIDE_STARTED");
                dispatchService.send(passengerId, "IN_APP", eventType, title, body, ride.getPublicId());
            }
        }
    }
}

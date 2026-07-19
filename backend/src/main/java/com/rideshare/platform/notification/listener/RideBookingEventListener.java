package com.rideshare.platform.notification.listener;

import com.rideshare.platform.booking.entity.Booking;
import com.rideshare.platform.booking.entity.BookingStatus;
import com.rideshare.platform.booking.repository.BookingRepository;
import com.rideshare.platform.config.KafkaTopics;
import com.rideshare.platform.kafka.events.*;
import com.rideshare.platform.notification.email.RideEmailService;
import com.rideshare.platform.notification.service.NotificationDispatchService;
import com.rideshare.platform.notification.sms.RideSmsService;
import com.rideshare.platform.ride.entity.Ride;
import com.rideshare.platform.ride.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR: Section 12 Notification Events - Ride Published, Booking Requested/Accepted/Cancelled,
 * Ride Started, Ride Completed. Consumes the domain events published by ride/booking services
 * and fans them out across notification channels: an in-app row (always), an email
 * (RideEmailService, gated by EMAIL_NOTIFICATIONS_ENABLED / RESEND_API_KEY - see
 * ResendEmailClient), and an SMS (RideSmsService, gated by SMS_NOTIFICATIONS_ENABLED /
 * MSG91_AUTH_KEY - see Msg91SmsClient) for every event that has a specific recipient.
 */
@Component
@RequiredArgsConstructor
public class RideBookingEventListener {

    private final NotificationDispatchService dispatchService;
    private final RideEmailService emailService;
    private final RideSmsService smsService;
    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;

    @KafkaListener(topics = KafkaTopics.RIDE_PUBLISHED, groupId = "notification-service")
    public void onRidePublished(RidePublishedEvent event) {
        // TODO: resolve subscribed passengers near the route and notify (Push/In-App)
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_REQUESTED, groupId = "notification-service")
    @Transactional(readOnly = true)
    public void onBookingRequested(BookingRequestedEvent event) {
        bookingRepository.findByPublicId(event.bookingPublicId()).ifPresent(booking -> {
            Ride ride = booking.getRide();
            dispatchService.send(ride.getDriver().getUser().getId(), "IN_APP", "BOOKING_REQUESTED",
                    "New booking request",
                    booking.getPassenger().getName() + " requested " + booking.getSeatsBooked()
                            + " seat" + (booking.getSeatsBooked() == 1 ? "" : "s") + " on your ride from "
                            + ride.getOriginAddress() + " to " + ride.getDestinationAddress() + ".",
                    ride.getPublicId());
            emailService.bookingRequested(booking);
            smsService.bookingRequested(booking);
        });
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_ACCEPTED, groupId = "notification-service")
    @Transactional(readOnly = true)
    public void onBookingAccepted(BookingAcceptedEvent event) {
        bookingRepository.findByPublicId(event.bookingPublicId()).ifPresent(booking -> {
            Ride ride = booking.getRide();
            dispatchService.send(booking.getPassenger().getId(), "IN_APP", "BOOKING_ACCEPTED",
                    "Your booking is confirmed",
                    "Your booking on the ride from " + ride.getOriginAddress() + " to "
                            + ride.getDestinationAddress() + " has been confirmed.",
                    ride.getPublicId());
            emailService.bookingConfirmed(booking);
            smsService.bookingConfirmed(booking);
        });
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_REJECTED, groupId = "notification-service")
    @Transactional(readOnly = true)
    public void onBookingRejected(BookingRejectedEvent event) {
        bookingRepository.findByPublicId(event.bookingPublicId()).ifPresent(booking -> {
            Ride ride = booking.getRide();
            dispatchService.send(booking.getPassenger().getId(), "IN_APP", "BOOKING_REJECTED",
                    "Booking not accepted",
                    "Your booking request on the ride from " + ride.getOriginAddress() + " to "
                            + ride.getDestinationAddress() + " wasn't accepted.",
                    ride.getPublicId());
            emailService.bookingRejected(booking);
            smsService.bookingRejected(booking);
        });
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_CANCELLED, groupId = "notification-service")
    @Transactional(readOnly = true)
    public void onBookingCancelled(BookingCancelledEvent event) {
        bookingRepository.findByPublicId(event.bookingPublicId()).ifPresent(booking -> {
            Ride ride = booking.getRide();
            String route = ride.getOriginAddress() + " to " + ride.getDestinationAddress();
            if ("PASSENGER".equals(event.cancelledBy())) {
                dispatchService.send(ride.getDriver().getUser().getId(), "IN_APP", "BOOKING_CANCELLED",
                        "Passenger cancelled their booking",
                        "A passenger cancelled their booking on your ride from " + route + ".",
                        ride.getPublicId());
            } else {
                dispatchService.send(booking.getPassenger().getId(), "IN_APP", "BOOKING_CANCELLED",
                        "Your booking was cancelled",
                        "Your booking for the ride from " + route + " was cancelled"
                                + ("DRIVER".equals(event.cancelledBy()) ? " by the driver." : ".")
                                + (event.reason() != null && !event.reason().isBlank() ? " Reason: " + event.reason() : ""),
                        ride.getPublicId());
            }
            emailService.bookingCancelled(booking, event.cancelledBy(), event.reason());
            smsService.bookingCancelled(booking, event.cancelledBy(), event.reason());
        });
    }

    @KafkaListener(topics = KafkaTopics.RIDE_STARTED, groupId = "notification-service")
    @Transactional(readOnly = true)
    public void onRideStarted(RideStartedEvent event) {
        rideRepository.findByPublicId(event.ridePublicId()).ifPresent(ride ->
                notifyConfirmedPassengers(ride, "RIDE_STARTED", "Your ride has started",
                        "Your driver has started the trip from " + ride.getOriginAddress()
                                + " to " + ride.getDestinationAddress() + ".",
                        booking -> { emailService.rideStarted(booking); smsService.rideStarted(booking); }));
    }

    @KafkaListener(topics = KafkaTopics.RIDE_COMPLETED, groupId = "notification-service")
    @Transactional(readOnly = true)
    public void onRideCompleted(RideCompletedEvent event) {
        rideRepository.findByPublicId(event.ridePublicId()).ifPresent(ride ->
                notifyCompletedPassengers(ride, "RIDE_COMPLETED", "Your ride is complete",
                        "Your trip from " + ride.getOriginAddress() + " to " + ride.getDestinationAddress()
                                + " has finished. Thanks for riding!"));
    }

    private void notifyConfirmedPassengers(Ride ride, String eventType, String title, String body,
                                            java.util.function.Consumer<Booking> emailAction) {
        for (Booking booking : bookingRepository.findByRideId(ride.getId())) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                dispatchService.send(booking.getPassenger().getId(), "IN_APP", eventType, title, body, ride.getPublicId());
                emailAction.accept(booking);
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
                emailService.rideCompleted(booking);
                smsService.rideCompleted(booking);
            }
        }
    }
}

package com.rideshare.platform.notification.email;

import com.rideshare.platform.booking.entity.Booking;
import com.rideshare.platform.notification.email.EmailTemplates.EmailContent;
import com.rideshare.platform.notification.email.EmailTemplates.RouteInfo;
import com.rideshare.platform.ride.entity.Ride;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Decides who gets emailed for which ride/booking event, and with what data. Deliberately
 * synchronous (not {@code @Async}) and called from within the same transactional context as the
 * Kafka listener that invokes it: the Ride/Booking associations it reads (driver, passenger,
 * vehicle) are lazy proxies that would throw LazyInitializationException if touched from a
 * different thread after the listener's transaction closed. {@link ResendEmailClient} itself
 * swallows and logs failures, so a slow or failing Resend call never breaks event processing.
 */
@Service
@RequiredArgsConstructor
public class RideEmailService {

    private final ResendEmailClient emailClient;
    private final EmailTemplates templates;

    public void bookingRequested(Booking booking) {
        Ride ride = booking.getRide();
        var driverUser = ride.getDriver().getUser();
        EmailContent content = templates.bookingRequested(driverUser.getName(), booking.getPassenger().getName(),
                rideRoute(ride), ride.getDepartureAt(), booking.getSeatsBooked(), booking.getFare());
        emailClient.send(driverUser.getEmail(), content.subject(), content.html());
    }

    public void bookingConfirmed(Booking booking) {
        Ride ride = booking.getRide();
        var passenger = booking.getPassenger();
        EmailContent content = templates.bookingConfirmed(passenger.getName(), ride.getDriver().getUser().getName(),
                ride.getVehicle() != null ? ride.getVehicle().getModel() : null,
                bookingRoute(booking), ride.getDepartureAt(), booking.getSeatsBooked(), booking.getFare());
        emailClient.send(passenger.getEmail(), content.subject(), content.html());
    }

    public void bookingRejected(Booking booking) {
        var passenger = booking.getPassenger();
        EmailContent content = templates.bookingRejected(passenger.getName(), bookingRoute(booking),
                booking.getRide().getDepartureAt(), booking.getSeatsBooked());
        emailClient.send(passenger.getEmail(), content.subject(), content.html());
    }

    /** Emails whichever side did NOT cancel - the driver if a passenger cancelled, the
     *  passenger if the driver (or the system) cancelled. */
    public void bookingCancelled(Booking booking, String cancelledBy, String reason) {
        Ride ride = booking.getRide();
        String recipientEmail;
        String recipientName;
        if ("PASSENGER".equals(cancelledBy)) {
            var driverUser = ride.getDriver().getUser();
            recipientEmail = driverUser.getEmail();
            recipientName = driverUser.getName();
        } else {
            var passenger = booking.getPassenger();
            recipientEmail = passenger.getEmail();
            recipientName = passenger.getName();
        }
        EmailContent content = templates.bookingCancelled(recipientName, cancelledBy, reason, bookingRoute(booking),
                ride.getDepartureAt(), booking.getSeatsBooked());
        emailClient.send(recipientEmail, content.subject(), content.html());
    }

    public void rideStarted(Booking booking) {
        Ride ride = booking.getRide();
        var passenger = booking.getPassenger();
        EmailContent content = templates.rideStarted(passenger.getName(), ride.getDriver().getUser().getName(),
                bookingRoute(booking), ride.getDepartureAt());
        emailClient.send(passenger.getEmail(), content.subject(), content.html());
    }

    public void rideCompleted(Booking booking) {
        var passenger = booking.getPassenger();
        EmailContent content = templates.rideCompleted(passenger.getName(), bookingRoute(booking),
                booking.getRide().getDepartureAt(), booking.getFare());
        emailClient.send(passenger.getEmail(), content.subject(), content.html());
    }

    private RouteInfo rideRoute(Ride ride) {
        return new RouteInfo(ride.getOriginAddress(), ride.getOriginLat(), ride.getOriginLng(),
                ride.getDestinationAddress(), ride.getDestinationLat(), ride.getDestinationLng());
    }

    private RouteInfo bookingRoute(Booking booking) {
        return new RouteInfo(booking.getPickupAddress(), booking.getPickupLat(), booking.getPickupLng(),
                booking.getDropAddress(), booking.getDropLat(), booking.getDropLng());
    }
}

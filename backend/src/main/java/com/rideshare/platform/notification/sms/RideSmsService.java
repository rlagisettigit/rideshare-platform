package com.rideshare.platform.notification.sms;

import com.rideshare.platform.booking.entity.Booking;
import com.rideshare.platform.ride.entity.Ride;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Decides who gets texted for which ride/booking/payment event, and with what copy. Mirrors
 * {@link com.rideshare.platform.notification.email.RideEmailService}: deliberately synchronous,
 * called from within the same transactional context as the Kafka listener (or, for the payment
 * alert, {@code BookingService.completeBookingsForRide}) that invokes it, so the lazy
 * Ride/Booking/User associations it reads are still safe to touch. {@link Msg91SmsClient}
 * swallows and logs its own failures, so a slow or failing MSG91 call never breaks event
 * processing.
 */
@Service
@RequiredArgsConstructor
public class RideSmsService {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM, h:mm a");

    private final Msg91SmsClient smsClient;

    public void bookingRequested(Booking booking) {
        Ride ride = booking.getRide();
        var driverUser = ride.getDriver().getUser();
        smsClient.sendSms(driverUser.getMobile(), booking.getPassenger().getName() + " requested "
                + booking.getSeatsBooked() + " seat" + (booking.getSeatsBooked() == 1 ? "" : "s")
                + " on your ride from " + ride.getOriginAddress() + " to " + ride.getDestinationAddress()
                + ". Open Aura Ride to respond.");
    }

    public void bookingConfirmed(Booking booking) {
        Ride ride = booking.getRide();
        var passenger = booking.getPassenger();
        smsClient.sendSms(passenger.getMobile(), "Booking confirmed! " + ride.getDriver().getUser().getName()
                + " will pick you up at " + ride.getDepartureAt().format(WHEN) + " from "
                + booking.getPickupAddress() + ". Fare: Rs " + rupees(booking) + ".");
    }

    public void bookingRejected(Booking booking) {
        Ride ride = booking.getRide();
        smsClient.sendSms(booking.getPassenger().getMobile(), "Your booking request on the ride from "
                + ride.getOriginAddress() + " to " + ride.getDestinationAddress()
                + " wasn't accepted. Find another ride on Aura Ride.");
    }

    /** Texts whichever side did NOT cancel - the driver if a passenger cancelled, the
     *  passenger if the driver (or the system) cancelled. */
    public void bookingCancelled(Booking booking, String cancelledBy, String reason) {
        Ride ride = booking.getRide();
        String recipientMobile;
        String message;
        if ("PASSENGER".equals(cancelledBy)) {
            recipientMobile = ride.getDriver().getUser().getMobile();
            message = booking.getPassenger().getName() + " cancelled their booking on your ride from "
                    + ride.getOriginAddress() + " to " + ride.getDestinationAddress() + ".";
        } else {
            recipientMobile = booking.getPassenger().getMobile();
            message = "Your booking for the ride from " + ride.getOriginAddress() + " to "
                    + ride.getDestinationAddress() + " was cancelled"
                    + ("DRIVER".equals(cancelledBy) ? " by the driver." : ".")
                    + (reason != null && !reason.isBlank() ? " Reason: " + reason : "");
        }
        smsClient.sendSms(recipientMobile, message);
    }

    public void rideStarted(Booking booking) {
        Ride ride = booking.getRide();
        smsClient.sendSms(booking.getPassenger().getMobile(), ride.getDriver().getUser().getName()
                + " has started the trip from " + ride.getOriginAddress() + " to "
                + ride.getDestinationAddress() + ". Track it live on Aura Ride.");
    }

    public void rideCompleted(Booking booking) {
        Ride ride = booking.getRide();
        smsClient.sendSms(booking.getPassenger().getMobile(), "Your trip from " + ride.getOriginAddress()
                + " to " + ride.getDestinationAddress() + " is complete. Fare: Rs " + rupees(booking)
                + ". Thanks for riding!");
    }

    public void paymentCompleted(Booking booking) {
        Ride ride = booking.getRide();
        smsClient.sendSms(booking.getPassenger().getMobile(), "Payment of Rs " + rupees(booking)
                + " for your ride from " + ride.getOriginAddress() + " to " + ride.getDestinationAddress()
                + " was successful.");
    }

    private String rupees(Booking booking) {
        return booking.getFare().setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}

package com.rideshare.platform.booking.service;

import com.rideshare.platform.booking.dto.BookingCreateRequest;
import com.rideshare.platform.booking.dto.BookingResponse;
import com.rideshare.platform.booking.dto.CancelBookingRequest;
import com.rideshare.platform.booking.entity.Booking;
import com.rideshare.platform.booking.entity.BookingStatus;
import com.rideshare.platform.booking.repository.BookingRepository;
import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.config.KafkaTopics;
import com.rideshare.platform.driver.entity.Driver;
import com.rideshare.platform.driver.service.DriverService;
import com.rideshare.platform.kafka.events.BookingAcceptedEvent;
import com.rideshare.platform.kafka.events.BookingCancelledEvent;
import com.rideshare.platform.kafka.events.BookingRejectedEvent;
import com.rideshare.platform.kafka.events.BookingRequestedEvent;
import com.rideshare.platform.notification.sms.RideSmsService;
import com.rideshare.platform.payment.service.PaymentService;
import com.rideshare.platform.pricing.dto.FareBreakdownResponse;
import com.rideshare.platform.pricing.service.FareBreakdown;
import com.rideshare.platform.pricing.service.RideFareEstimator;
import com.rideshare.platform.ride.entity.Ride;
import com.rideshare.platform.ride.entity.RideStatus;
import com.rideshare.platform.ride.repository.RecurringRideRepository;
import com.rideshare.platform.ride.repository.RideRepository;
import com.rideshare.platform.route.entity.RideRoutePoint;
import com.rideshare.platform.route.repository.RideRoutePointRepository;
import com.rideshare.platform.route.service.H3Service;
import com.rideshare.platform.user.entity.User;
import com.rideshare.platform.user.repository.UserRepository;
import com.rideshare.platform.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * FR: Section 9 Booking Management.
 * Booking Validation: ride active, seats available, passenger verified, no duplicate booking.
 * Seat Allocation: reserved atomically via pessimistic row lock on Ride - no overselling.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final RecurringRideRepository recurringRideRepository;
    private final UserRepository userRepository;
    private final RideRoutePointRepository rideRoutePointRepository;
    private final H3Service h3Service;
    private final DriverService driverService;
    private final PaymentService paymentService;
    private final RideSmsService rideSmsService;
    private final WalletService walletService;
    private final RideFareEstimator rideFareEstimator;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    @Transactional
    public BookingResponse createBooking(String userPublicId, BookingCreateRequest request) {
        return createBooking(userPublicId, request, null);
    }

    /**
     * @param bookingBatchId set by RecurringRideService.bookAll() to correlate every booking
     *                       created in one "book all upcoming occurrences" action, so the
     *                       driver can accept/reject the whole batch together; null for a
     *                       normal single-ride booking.
     */
    @Transactional
    public BookingResponse createBooking(String userPublicId, BookingCreateRequest request, String bookingBatchId) {
        User passenger = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> ApiException.notFound("USER_001", "User not found."));

        Ride ride = rideRepository.findByPublicId(request.ridePublicId())
                .orElseThrow(() -> ApiException.notFound("RIDE_006", "Ride not found."));

        // Re-fetch with a pessimistic lock to make seat allocation atomic under concurrent bookings.
        Ride lockedRide = rideRepository.findByIdForUpdate(ride.getId())
                .orElseThrow(() -> ApiException.notFound("RIDE_006", "Ride not found."));

        if (lockedRide.getStatus() != RideStatus.ACTIVE) {
            throw ApiException.businessRule("BOOKING_001", "Ride is not accepting bookings.");
        }
        if (lockedRide.getAvailableSeats() < request.seats()) {
            throw ApiException.businessRule("RIDE_001", "Available seats exceeded.");
        }
        if (bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(lockedRide.getId(), passenger.getId(), ACTIVE_STATUSES)) {
            throw ApiException.conflict("BOOKING_002", "You already have an active booking on this ride.");
        }

        List<RideRoutePoint> points = rideRoutePointRepository.findByRideIdOrderBySequenceNoAsc(lockedRide.getId());
        RideRoutePoint pickupPoint = nearestPoint(points, request.pickupLat(), request.pickupLng());
        RideRoutePoint dropPoint = nearestPoint(points, request.dropLat(), request.dropLng());
        if (pickupPoint == null || dropPoint == null || pickupPoint.getSequenceNo() >= dropPoint.getSequenceNo()) {
            throw ApiException.badRequest("BOOKING_003", "Pickup and drop points are not valid along this ride's route.");
        }

        // No overselling: decrement happens on the locked row within this transaction.
        lockedRide.setAvailableSeats(lockedRide.getAvailableSeats() - request.seats());
        rideRepository.save(lockedRide);

        FareBreakdown fareBreakdown = rideFareEstimator.estimate(lockedRide, pickupPoint, dropPoint, request.seats());

        Booking booking = new Booking();
        booking.setRide(lockedRide);
        booking.setPassenger(passenger);
        booking.setPickupLat(request.pickupLat());
        booking.setPickupLng(request.pickupLng());
        booking.setPickupAddress(request.pickupAddress());
        booking.setPickupSequenceNo(pickupPoint.getSequenceNo());
        booking.setDropLat(request.dropLat());
        booking.setDropLng(request.dropLng());
        booking.setDropAddress(request.dropAddress());
        booking.setDropSequenceNo(dropPoint.getSequenceNo());
        booking.setSeatsBooked(request.seats());
        booking.setFare(fareBreakdown.passengerFare());
        booking.setStatus(BookingStatus.PENDING);
        booking.setBookingBatchId(bookingBatchId);
        bookingRepository.save(booking);

        kafkaTemplate.send(KafkaTopics.BOOKING_REQUESTED,
                new BookingRequestedEvent(booking.getPublicId(), lockedRide.getPublicId(), passenger.getPublicId(), request.seats()));

        return toResponse(booking);
    }

    @Transactional
    public BookingResponse respondToBooking(String driverUserPublicId, String bookingPublicId, boolean accept) {
        Booking booking = bookingRepository.findByPublicId(bookingPublicId)
                .orElseThrow(() -> ApiException.notFound("BOOKING_004", "Booking not found."));

        if (!booking.getRide().getDriver().getUser().getPublicId().equals(driverUserPublicId)) {
            throw ApiException.forbidden("BOOKING_005", "You do not own the ride for this booking.");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw ApiException.businessRule("BOOKING_006", "Booking has already been responded to.");
        }

        if (accept) {
            booking.setStatus(BookingStatus.CONFIRMED);
        } else {
            booking.setStatus(BookingStatus.REJECTED);
            restoreSeats(booking);
        }
        bookingRepository.save(booking);

        String passengerPublicId = booking.getPassenger().getPublicId();
        if (accept) {
            kafkaTemplate.send(KafkaTopics.BOOKING_ACCEPTED,
                    new BookingAcceptedEvent(booking.getPublicId(), booking.getRide().getPublicId(), passengerPublicId));
        } else {
            kafkaTemplate.send(KafkaTopics.BOOKING_REJECTED,
                    new BookingRejectedEvent(booking.getPublicId(), booking.getRide().getPublicId(), passengerPublicId));
        }

        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancel(String userPublicId, String bookingPublicId, CancelBookingRequest request) {
        Booking booking = bookingRepository.findByPublicId(bookingPublicId)
                .orElseThrow(() -> ApiException.notFound("BOOKING_004", "Booking not found."));

        boolean isPassenger = booking.getPassenger().getPublicId().equals(userPublicId);
        boolean isDriver = booking.getRide().getDriver().getUser().getPublicId().equals(userPublicId);
        if (!isPassenger && !isDriver) {
            throw ApiException.forbidden("BOOKING_007", "You are not part of this booking.");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw ApiException.businessRule("BOOKING_008", "Booking cannot be cancelled in its current state.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledBy(isPassenger ? "PASSENGER" : "DRIVER");
        booking.setCancellationReason(request == null ? null : request.reason());
        restoreSeats(booking);
        bookingRepository.save(booking);

        // Refund policy is configurable (system_configuration.booking.refund_policy) and
        // handled asynchronously by the payment/refund worker consuming this event.
        kafkaTemplate.send(KafkaTopics.BOOKING_CANCELLED,
                new BookingCancelledEvent(booking.getPublicId(), booking.getRide().getPublicId(),
                        booking.getCancelledBy(), booking.getCancellationReason()));

        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(String userPublicId) {
        User passenger = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> ApiException.notFound("USER_001", "User not found."));
        return bookingRepository.findByPassengerIdOrderByCreatedAtDesc(passenger.getId())
                .stream().map(this::toResponse).toList();
    }

    /** Used for the "view ride details" popup from a wallet transaction/payment record. */
    @Transactional(readOnly = true)
    public BookingResponse getByPublicId(String userPublicId, String bookingPublicId) {
        Booking booking = bookingRepository.findByPublicId(bookingPublicId)
                .orElseThrow(() -> ApiException.notFound("BOOKING_004", "Booking not found."));
        boolean isPassenger = booking.getPassenger().getPublicId().equals(userPublicId);
        boolean isDriver = booking.getRide().getDriver().getUser().getPublicId().equals(userPublicId);
        if (!isPassenger && !isDriver) {
            throw ApiException.forbidden("BOOKING_009", "You are not part of this booking.");
        }
        return toResponse(booking);
    }

    /** Booking requests (any status) across every ride this driver has published. */
    @Transactional(readOnly = true)
    public List<BookingResponse> driverBookingRequests(String driverUserPublicId) {
        Driver driver = driverService.getVerifiedDriver(driverUserPublicId);
        return bookingRepository.findByRideDriverIdOrderByCreatedAtDesc(driver.getId())
                .stream().map(this::toResponse).toList();
    }

    /**
     * Called by RideService.finish() once the driver marks a ride FINISHED: completes every
     * confirmed booking and settles its fare (payment capture + driver wallet credit), and
     * system-cancels any booking the driver never responded to - there's no seat restore or
     * payment reversal needed there since no payment was ever taken for a PENDING booking.
     */
    @Transactional
    public void completeBookingsForRide(Long rideId) {
        for (Booking booking : bookingRepository.findByRideId(rideId)) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                booking.setStatus(BookingStatus.COMPLETED);
                bookingRepository.save(booking);

                paymentService.capture(booking.getId(), booking.getFare(), "ride-completion:" + booking.getPublicId());
                rideSmsService.paymentCompleted(booking);
                walletService.credit(booking.getRide().getDriver().getUser().getId(), booking.getFare(),
                        "RIDE_FARE", booking.getPublicId());
            } else if (booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancelledBy("SYSTEM");
                booking.setCancellationReason("Ride completed before the booking was responded to.");
                bookingRepository.save(booking);
            }
        }
    }

    /**
     * Called by RideService.cancel() when a driver cancels an entire ride: cancels every
     * booking still awaiting a response or already confirmed, restores seats, and fires
     * BOOKING_CANCELLED per booking so affected passengers get notified.
     */
    @Transactional
    public void cancelBookingsForRide(Long rideId) {
        for (Booking booking : bookingRepository.findByRideId(rideId)) {
            if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.CONFIRMED) {
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancelledBy("DRIVER");
                booking.setCancellationReason("The driver cancelled this ride.");
                restoreSeats(booking);
                bookingRepository.save(booking);

                kafkaTemplate.send(KafkaTopics.BOOKING_CANCELLED,
                        new BookingCancelledEvent(booking.getPublicId(), booking.getRide().getPublicId(),
                                booking.getCancelledBy(), booking.getCancellationReason()));
            }
        }
    }

    private void restoreSeats(Booking booking) {
        Ride ride = rideRepository.findByIdForUpdate(booking.getRide().getId())
                .orElseThrow(() -> ApiException.notFound("RIDE_006", "Ride not found."));
        ride.setAvailableSeats(ride.getAvailableSeats() + booking.getSeatsBooked());
        rideRepository.save(ride);
    }

    /**
     * Lets a passenger see the price for their actual pickup/drop on a real ride before they
     * commit to booking - same fare math as createBooking, but read-only: no seat lock, no
     * Booking row written. FareBreakdownResponse carries a disclaimer that this is an estimate.
     */
    @Transactional(readOnly = true)
    public FareBreakdownResponse previewFare(BookingCreateRequest request) {
        Ride ride = rideRepository.findByPublicId(request.ridePublicId())
                .orElseThrow(() -> ApiException.notFound("RIDE_006", "Ride not found."));

        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw ApiException.businessRule("BOOKING_001", "Ride is not accepting bookings.");
        }
        if (ride.getAvailableSeats() < request.seats()) {
            throw ApiException.businessRule("RIDE_001", "Available seats exceeded.");
        }

        List<RideRoutePoint> points = rideRoutePointRepository.findByRideIdOrderBySequenceNoAsc(ride.getId());
        RideRoutePoint pickupPoint = nearestPoint(points, request.pickupLat(), request.pickupLng());
        RideRoutePoint dropPoint = nearestPoint(points, request.dropLat(), request.dropLng());
        if (pickupPoint == null || dropPoint == null || pickupPoint.getSequenceNo() >= dropPoint.getSequenceNo()) {
            throw ApiException.badRequest("BOOKING_003", "Pickup and drop points are not valid along this ride's route.");
        }

        return FareBreakdownResponse.from(rideFareEstimator.estimate(ride, pickupPoint, dropPoint, request.seats()));
    }

    private RideRoutePoint nearestPoint(List<RideRoutePoint> points, double lat, double lng) {
        String targetCell = h3Service.latLngToCell(lat, lng);
        return points.stream()
                .min(Comparator.comparingDouble(p -> h3Service.gridDistanceKm(targetCell, p.getH3Cell())))
                .orElse(null);
    }

    private BookingResponse toResponse(Booking b) {
        Long recurringRideId = b.getRide().getRecurringRideId();
        String recurringRidePublicId = recurringRideId == null ? null
                : recurringRideRepository.findById(recurringRideId).map(rr -> rr.getPublicId()).orElse(null);
        return new BookingResponse(b.getPublicId(), b.getRide().getPublicId(), b.getStatus().name(),
                b.getSeatsBooked(), b.getFare(), b.getPickupAddress(), b.getPickupLat(), b.getPickupLng(),
                b.getDropAddress(), b.getDropLat(), b.getDropLng(),
                b.getPassenger().getName(), b.getPassenger().getPublicId(), b.getRide().getOriginAddress(),
                b.getRide().getDestinationAddress(), b.getRide().getDepartureAt(), b.getRide().getStatus().name(),
                b.getBookingBatchId(), recurringRidePublicId);
    }
}

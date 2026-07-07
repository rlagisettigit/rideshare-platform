package com.rideshare.platform.ride.service;

import com.rideshare.platform.booking.service.BookingService;
import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.config.KafkaTopics;
import com.rideshare.platform.driver.entity.Driver;
import com.rideshare.platform.driver.service.DriverService;
import com.rideshare.platform.kafka.events.RideCompletedEvent;
import com.rideshare.platform.kafka.events.RidePublishedEvent;
import com.rideshare.platform.kafka.events.RideStartedEvent;
import com.rideshare.platform.pricing.dto.FareBreakdownResponse;
import com.rideshare.platform.pricing.service.FareBreakdown;
import com.rideshare.platform.pricing.service.RideFareEstimator;
import com.rideshare.platform.ride.dto.RideCreateRequest;
import com.rideshare.platform.ride.dto.RideResponse;
import com.rideshare.platform.ride.dto.RideStopPlanResponse;
import com.rideshare.platform.ride.entity.Ride;
import com.rideshare.platform.ride.entity.RideStatus;
import com.rideshare.platform.ride.repository.RecurringRideRepository;
import com.rideshare.platform.ride.repository.RideRepository;
import com.rideshare.platform.route.entity.RideRoutePoint;
import com.rideshare.platform.route.repository.RideRoutePointRepository;
import com.rideshare.platform.route.service.RouteService;
import com.rideshare.platform.vehicle.entity.Vehicle;
import com.rideshare.platform.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FR: Section 6 Ride Management + Section 7 Route Management.
 * Ride Validation: departure > now, vehicle verified, driver verified, seats > 0, route generated successfully.
 */
@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final DriverService driverService;
    private final VehicleService vehicleService;
    private final RouteService routeService;
    private final BookingService bookingService;
    private final RecurringRideRepository recurringRideRepository;
    private final RideRoutePointRepository rideRoutePointRepository;
    private final RideFareEstimator rideFareEstimator;
    private final RideStopPlanningService rideStopPlanningService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public RideResponse publish(String userPublicId, RideCreateRequest request) {
        LocalDateTime departureAt = LocalDateTime.of(request.departureDate(), request.departureTime());
        if (!departureAt.isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("RIDE_002", "Departure time must be in the future.");
        }

        // FR: "Online drivers can publish rides" + driver must be VERIFIED.
        // Publishing is itself a declaration of availability, so a driver who isn't online yet
        // is brought online here rather than being blocked - this is what makes "offer a ride
        // departing in a few minutes" work without a separate manual step first.
        Driver driver = driverService.getVerifiedDriver(userPublicId);
        driverService.markOnline(driver);

        // FR: Vehicle Verified
        Vehicle vehicle = vehicleService.getApprovedVehicle(request.vehicleId());
        if (!vehicle.getDriver().getId().equals(driver.getId())) {
            throw ApiException.forbidden("RIDE_004", "Vehicle does not belong to this driver.");
        }
        if (request.availableSeats() > vehicle.getSeatingCapacity()) {
            throw ApiException.badRequest("RIDE_005", "Available seats exceed vehicle capacity.");
        }

        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setVehicle(vehicle);
        ride.setOriginAddress(request.originAddress());
        ride.setOriginLat(request.originLat());
        ride.setOriginLng(request.originLng());
        ride.setDestinationAddress(request.destinationAddress());
        ride.setDestinationLat(request.destinationLat());
        ride.setDestinationLng(request.destinationLng());
        ride.setDepartureDate(request.departureDate());
        ride.setDepartureTime(request.departureTime());
        ride.setDepartureAt(departureAt);
        ride.setTotalSeats(request.availableSeats());
        ride.setAvailableSeats(request.availableSeats());
        ride.setPricePerSeat(request.pricePerSeat());
        ride.setLuggageAllowed(request.luggageAllowed());
        ride.setSmokingAllowed(request.smokingAllowed());
        ride.setMusicPreference(request.musicPreference());
        ride.setWomenOnly(request.womenOnly());
        ride.setPetsAllowed(request.petsAllowed());
        ride.setDescription(request.description());
        if (request.maxDetourKm() != null) {
            ride.setMaxDetourKm(request.maxDetourKm());
        }
        ride.setStatus(RideStatus.PENDING);
        rideRepository.save(ride);

        // FR: Section 7 Route Management pipeline - route must generate successfully or the publish fails.
        // If the driver picked a specific option from the route preview, store that real
        // road-following route instead of the default locally-interpolated straight line.
        if (request.selectedRoutePolyline() != null && !request.selectedRoutePolyline().isBlank()) {
            routeService.storeSelectedRoute(ride.getId(),
                    request.selectedRouteProvider() != null ? request.selectedRouteProvider() : "DRIVER_SELECTED",
                    request.selectedRoutePolyline(),
                    request.selectedRouteDistanceMeters() != null ? request.selectedRouteDistanceMeters() : 0,
                    request.selectedRouteDurationSeconds() != null ? request.selectedRouteDurationSeconds() : 0);
        } else {
            routeService.generateAndStoreRoute(ride.getId(), ride.getOriginLat(), ride.getOriginLng(),
                    ride.getDestinationLat(), ride.getDestinationLng());
        }

        ride.setRouteGenerated(true);
        ride.setStatus(RideStatus.ACTIVE);
        rideRepository.save(ride);

        kafkaTemplate.send(KafkaTopics.RIDE_PUBLISHED,
                new RidePublishedEvent(ride.getPublicId(), driver.getUser().getPublicId(), departureAt));

        return toResponse(ride);
    }

    @Transactional(readOnly = true)
    public RideResponse getByPublicId(String publicId) {
        return toResponse(findByPublicId(publicId));
    }

    public Ride findByPublicId(String publicId) {
        return rideRepository.findByPublicId(publicId)
                .orElseThrow(() -> ApiException.notFound("RIDE_006", "Ride not found."));
    }

    @Transactional(readOnly = true)
    public List<RideResponse> myRides(String userPublicId) {
        Driver driver = driverService.getVerifiedDriver(userPublicId);
        return rideRepository.findByDriverIdOrderByDepartureAtDesc(driver.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public RideResponse cancel(String userPublicId, String ridePublicId) {
        Ride ride = findByPublicId(ridePublicId);
        Driver driver = driverService.getVerifiedDriver(userPublicId);
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw ApiException.forbidden("RIDE_007", "You do not own this ride.");
        }
        if (ride.getStatus() == RideStatus.FINISHED || ride.getStatus() == RideStatus.CANCELLED
                || ride.getStatus() == RideStatus.IN_PROGRESS) {
            throw ApiException.businessRule("RIDE_008", "Ride cannot be cancelled in its current state.");
        }
        ride.setStatus(RideStatus.CANCELLED);
        rideRepository.save(ride);
        routeService.removeRouteIndex(ride.getId());
        bookingService.cancelBookingsForRide(ride.getId());
        return toResponse(ride);
    }

    /** Driver marks the trip as actually underway. No departure-time gating - a driver can
     *  start as soon as they're ready, matching the "offer a ride starting in a few minutes"
     *  use case. */
    @Transactional
    public RideResponse start(String userPublicId, String ridePublicId) {
        Ride ride = findByPublicId(ridePublicId);
        Driver driver = driverService.getVerifiedDriver(userPublicId);
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw ApiException.forbidden("RIDE_009", "You do not own this ride.");
        }
        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw ApiException.businessRule("RIDE_010", "Ride must be ACTIVE to start.");
        }
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setActualStartTime(LocalDateTime.now());
        rideRepository.save(ride);

        // FR: Pickup/Drop-off Ordering - reorders confirmed passengers into the most efficient
        // visiting sequence rather than booking order, so the driver has a plan the moment the
        // trip starts.
        rideStopPlanningService.planAndPersist(ride, userPublicId);

        publishAfterCommit(KafkaTopics.RIDE_STARTED, new RideStartedEvent(ride.getPublicId()));
        return toResponse(ride);
    }

    @Transactional(readOnly = true)
    public RideStopPlanResponse getStopPlan(String userPublicId, String ridePublicId) {
        return rideStopPlanningService.getStopPlan(userPublicId, ridePublicId);
    }

    /** Driver marks the trip as finished: completes every confirmed booking (settling fare and
     *  crediting the driver's wallet) and system-cancels any booking still awaiting a response. */
    @Transactional
    public RideResponse finish(String userPublicId, String ridePublicId) {
        Ride ride = findByPublicId(ridePublicId);
        Driver driver = driverService.getVerifiedDriver(userPublicId);
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw ApiException.forbidden("RIDE_011", "You do not own this ride.");
        }
        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw ApiException.businessRule("RIDE_012", "Ride must be IN_PROGRESS to finish.");
        }
        ride.setStatus(RideStatus.FINISHED);
        ride.setActualEndTime(LocalDateTime.now());
        rideRepository.save(ride);

        bookingService.completeBookingsForRide(ride.getId());

        publishAfterCommit(KafkaTopics.RIDE_COMPLETED, new RideCompletedEvent(ride.getPublicId()));
        return toResponse(ride);
    }

    /**
     * Publishing inside the same transaction that wrote the data a consumer depends on is a
     * race: the notification listener can read the row before this transaction commits (this
     * is exactly what silently dropped the RIDE_COMPLETED notification - it queried for
     * COMPLETED bookings before completeBookingsForRide()'s write was durable). Deferring to
     * afterCommit() guarantees consumers only see state that's actually been committed.
     */
    private void publishAfterCommit(String topic, Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    kafkaTemplate.send(topic, event);
                }
            });
        } else {
            kafkaTemplate.send(topic, event);
        }
    }

    private RideResponse toResponse(Ride r) {
        String recurringRidePublicId = r.getRecurringRideId() == null ? null
                : recurringRideRepository.findById(r.getRecurringRideId()).map(rr -> rr.getPublicId()).orElse(null);
        return new RideResponse(
                r.getPublicId(),
                r.getDriver().getUser().getName(),
                r.getDriver().getUser().getAverageRating() == null ? null : r.getDriver().getUser().getAverageRating().doubleValue(),
                r.getVehicle().getModel(),
                r.getOriginAddress(),
                r.getDestinationAddress(),
                r.getDepartureAt(),
                r.getAvailableSeats(),
                r.getPricePerSeat(),
                estimatedFarePerSeat(r),
                FareBreakdownResponse.DISCLAIMER,
                r.isWomenOnly(),
                r.isPetsAllowed(),
                r.isLuggageAllowed(),
                r.getStatus().name(),
                recurringRidePublicId
        );
    }

    /**
     * PricingEngine quote for one seat over the ride's full origin-to-destination route -
     * there's no passenger-specific pickup/drop on a ride-details view, so this is a
     * representative "starting from" price, not what any particular booking will be charged.
     */
    private BigDecimal estimatedFarePerSeat(Ride ride) {
        List<RideRoutePoint> points = rideRoutePointRepository.findByRideIdOrderBySequenceNoAsc(ride.getId());
        if (points.size() < 2) {
            return ride.getPricePerSeat();
        }
        FareBreakdown breakdown = rideFareEstimator.estimate(ride, points.get(0), points.get(points.size() - 1), 1);
        return breakdown.passengerFare();
    }
}

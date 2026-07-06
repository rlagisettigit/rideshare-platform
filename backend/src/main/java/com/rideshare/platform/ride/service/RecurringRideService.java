package com.rideshare.platform.ride.service;

import com.rideshare.platform.booking.dto.BookingBatchSummary;
import com.rideshare.platform.booking.dto.BookingCreateRequest;
import com.rideshare.platform.booking.dto.BookingResponse;
import com.rideshare.platform.booking.entity.Booking;
import com.rideshare.platform.booking.repository.BookingRepository;
import com.rideshare.platform.booking.service.BookingService;
import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.driver.entity.Driver;
import com.rideshare.platform.driver.service.DriverService;
import com.rideshare.platform.ride.dto.RecurringBookingRequest;
import com.rideshare.platform.ride.dto.RecurringBookingSummary;
import com.rideshare.platform.ride.dto.RecurringOccurrenceResponse;
import com.rideshare.platform.ride.dto.RecurringRideCreateRequest;
import com.rideshare.platform.ride.dto.RecurringRideResponse;
import com.rideshare.platform.ride.entity.RecurringRide;
import com.rideshare.platform.ride.entity.RecurringRideStatus;
import com.rideshare.platform.ride.entity.Ride;
import com.rideshare.platform.ride.entity.RideStatus;
import com.rideshare.platform.ride.repository.RecurringRideRepository;
import com.rideshare.platform.ride.repository.RideRepository;
import com.rideshare.platform.route.provider.RouteResult;
import com.rideshare.platform.route.service.RouteService;
import com.rideshare.platform.vehicle.entity.Vehicle;
import com.rideshare.platform.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FR: recurring rides - a driver-defined schedule (e.g. "every weekday at 9am") that
 * immediately generates one independently bookable Ride per matching date, reusing the exact
 * same publish/search/booking pipeline as a one-off ride (so search, booking, accept, start,
 * finish, payment, wallet, and live tracking all work unchanged). The route is resolved once
 * for the whole series (see RouteService.resolveRoute) rather than once per occurrence, since
 * every occurrence shares an identical origin/destination.
 */
@Service
@RequiredArgsConstructor
public class RecurringRideService {

    /** Bounds how far a series can run so a single request can't accidentally spawn hundreds
     *  of rides - a fresh series covering more ground is just a matter of creating another one. */
    private static final int MAX_RANGE_DAYS = 90;

    private final RecurringRideRepository recurringRideRepository;
    private final RideRepository rideRepository;
    private final DriverService driverService;
    private final VehicleService vehicleService;
    private final RouteService routeService;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    @Transactional
    public RecurringRideResponse create(String userPublicId, RecurringRideCreateRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw ApiException.badRequest("RECURRING_001", "End date must be on or after the start date.");
        }
        if (ChronoUnit.DAYS.between(request.startDate(), request.endDate()) > MAX_RANGE_DAYS) {
            throw ApiException.badRequest("RECURRING_002",
                    "Recurring rides can span at most " + MAX_RANGE_DAYS + " days - create a new series to extend further.");
        }

        Driver driver = driverService.getVerifiedDriver(userPublicId);
        driverService.markOnline(driver);
        Vehicle vehicle = vehicleService.getApprovedVehicle(request.vehicleId());
        if (!vehicle.getDriver().getId().equals(driver.getId())) {
            throw ApiException.forbidden("RECURRING_003", "Vehicle does not belong to this driver.");
        }
        if (request.availableSeats() > vehicle.getSeatingCapacity()) {
            throw ApiException.badRequest("RECURRING_004", "Available seats exceed vehicle capacity.");
        }

        List<LocalDate> occurrenceDates = occurrenceDates(request.startDate(), request.endDate(), request.daysOfWeek());
        LocalDateTime now = LocalDateTime.now();
        occurrenceDates.removeIf(date -> !LocalDateTime.of(date, request.departureTime()).isAfter(now));
        if (occurrenceDates.isEmpty()) {
            throw ApiException.badRequest("RECURRING_005", "No matching dates in the future for the selected days and range.");
        }

        RecurringRide series = new RecurringRide();
        series.setDriver(driver);
        series.setVehicle(vehicle);
        series.setOriginAddress(request.originAddress());
        series.setOriginLat(request.originLat());
        series.setOriginLng(request.originLng());
        series.setDestinationAddress(request.destinationAddress());
        series.setDestinationLat(request.destinationLat());
        series.setDestinationLng(request.destinationLng());
        series.setDaysOfWeek(request.daysOfWeek().stream().map(Enum::name).collect(Collectors.joining(",")));
        series.setDepartureTime(request.departureTime());
        series.setStartDate(request.startDate());
        series.setEndDate(request.endDate());
        series.setTotalSeats(request.availableSeats());
        series.setPricePerSeat(request.pricePerSeat());
        series.setLuggageAllowed(request.luggageAllowed());
        series.setSmokingAllowed(request.smokingAllowed());
        series.setWomenOnly(request.womenOnly());
        series.setPetsAllowed(request.petsAllowed());
        series.setDescription(request.description());
        if (request.maxDetourKm() != null) {
            series.setMaxDetourKm(request.maxDetourKm());
        }
        series.setStatus(RecurringRideStatus.ACTIVE);
        recurringRideRepository.save(series);

        boolean hasSelectedRoute = request.selectedRoutePolyline() != null && !request.selectedRoutePolyline().isBlank();
        RouteResult resolvedRoute = hasSelectedRoute ? null
                : routeService.resolveRoute(request.originLat(), request.originLng(), request.destinationLat(), request.destinationLng());

        for (LocalDate date : occurrenceDates) {
            Ride ride = new Ride();
            ride.setDriver(driver);
            ride.setVehicle(vehicle);
            ride.setOriginAddress(request.originAddress());
            ride.setOriginLat(request.originLat());
            ride.setOriginLng(request.originLng());
            ride.setDestinationAddress(request.destinationAddress());
            ride.setDestinationLat(request.destinationLat());
            ride.setDestinationLng(request.destinationLng());
            ride.setDepartureDate(date);
            ride.setDepartureTime(request.departureTime());
            ride.setDepartureAt(LocalDateTime.of(date, request.departureTime()));
            ride.setTotalSeats(request.availableSeats());
            ride.setAvailableSeats(request.availableSeats());
            ride.setPricePerSeat(request.pricePerSeat());
            ride.setLuggageAllowed(request.luggageAllowed());
            ride.setSmokingAllowed(request.smokingAllowed());
            ride.setWomenOnly(request.womenOnly());
            ride.setPetsAllowed(request.petsAllowed());
            ride.setDescription(request.description());
            if (request.maxDetourKm() != null) {
                ride.setMaxDetourKm(request.maxDetourKm());
            }
            ride.setRecurringRideId(series.getId());
            ride.setStatus(RideStatus.PENDING);
            rideRepository.save(ride);

            if (hasSelectedRoute) {
                routeService.storeSelectedRoute(ride.getId(),
                        request.selectedRouteProvider() != null ? request.selectedRouteProvider() : "DRIVER_SELECTED",
                        request.selectedRoutePolyline(),
                        request.selectedRouteDistanceMeters() != null ? request.selectedRouteDistanceMeters() : 0,
                        request.selectedRouteDurationSeconds() != null ? request.selectedRouteDurationSeconds() : 0);
            } else {
                routeService.storeRoute(ride.getId(), resolvedRoute);
            }

            ride.setRouteGenerated(true);
            ride.setStatus(RideStatus.ACTIVE);
            rideRepository.save(ride);
        }

        return toResponse(series);
    }

    @Transactional(readOnly = true)
    public List<RecurringRideResponse> myRecurringRides(String userPublicId) {
        Driver driver = driverService.getVerifiedDriver(userPublicId);
        return recurringRideRepository.findByDriverIdOrderByStartDateDesc(driver.getId())
                .stream().map(this::toResponse).toList();
    }

    /** Cancels the series and every occurrence that hasn't started/finished yet - bookings on
     *  occurrences already IN_PROGRESS or FINISHED are left untouched, same as a one-off ride. */
    @Transactional
    public RecurringRideResponse cancel(String userPublicId, String recurringRidePublicId) {
        RecurringRide series = recurringRideRepository.findByPublicId(recurringRidePublicId)
                .orElseThrow(() -> ApiException.notFound("RECURRING_006", "Recurring ride not found."));
        Driver driver = driverService.getVerifiedDriver(userPublicId);
        if (!series.getDriver().getId().equals(driver.getId())) {
            throw ApiException.forbidden("RECURRING_007", "You do not own this recurring ride.");
        }

        series.setStatus(RecurringRideStatus.CANCELLED);
        recurringRideRepository.save(series);

        List<Ride> occurrences = rideRepository.findByRecurringRideId(series.getId());
        for (Ride ride : occurrences) {
            if (ride.getStatus() == RideStatus.PENDING || ride.getStatus() == RideStatus.ACTIVE) {
                ride.setStatus(RideStatus.CANCELLED);
                rideRepository.save(ride);
                routeService.removeRouteIndex(ride.getId());
            }
        }

        return toResponse(series);
    }

    /** Upcoming bookable dates for a series, so a passenger can pick which ones they actually
     *  want (e.g. Mon/Wed/Fri out of a Mon-Fri series) before booking. */
    @Transactional(readOnly = true)
    public List<RecurringOccurrenceResponse> getOccurrences(String recurringRidePublicId) {
        RecurringRide series = recurringRideRepository.findByPublicId(recurringRidePublicId)
                .orElseThrow(() -> ApiException.notFound("RECURRING_006", "Recurring ride not found."));
        LocalDateTime now = LocalDateTime.now();
        return rideRepository.findByRecurringRideId(series.getId()).stream()
                .filter(r -> r.getStatus() == RideStatus.ACTIVE)
                .filter(r -> r.getDepartureAt().isAfter(now))
                .sorted(Comparator.comparing(Ride::getDepartureAt))
                .map(r -> new RecurringOccurrenceResponse(r.getPublicId(), r.getDepartureDate(), r.getDepartureAt(), r.getAvailableSeats()))
                .toList();
    }

    /**
     * Books a passenger onto occurrences of a series in one action, since the whole point of a
     * recurring ride is not having to book each date one at a time. If request.ridePublicIds()
     * is empty, every upcoming occurrence is booked; otherwise only the ones specified. Each
     * occurrence is booked independently via the normal single-ride booking path (seat
     * allocation, duplicate-booking guard, fare calc all reused as-is) - deliberately NOT
     * wrapped in one transaction, so one occurrence failing (full, already booked, etc.)
     * doesn't roll back the occurrences that already succeeded. All bookings created here share
     * one bookingBatchId so the driver can accept/reject the whole batch together.
     */
    public RecurringBookingSummary bookAll(String passengerUserPublicId, String recurringRidePublicId, RecurringBookingRequest request) {
        RecurringRide series = recurringRideRepository.findByPublicId(recurringRidePublicId)
                .orElseThrow(() -> ApiException.notFound("RECURRING_006", "Recurring ride not found."));
        if (series.getStatus() != RecurringRideStatus.ACTIVE) {
            throw ApiException.businessRule("RECURRING_008", "This recurring ride has been cancelled.");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Ride> occurrences = rideRepository.findByRecurringRideId(series.getId()).stream()
                .filter(r -> r.getStatus() == RideStatus.ACTIVE)
                .filter(r -> r.getDepartureAt().isAfter(now))
                .sorted(Comparator.comparing(Ride::getDepartureAt))
                .toList();

        if (request.ridePublicIds() != null && !request.ridePublicIds().isEmpty()) {
            Set<String> selected = Set.copyOf(request.ridePublicIds());
            occurrences = occurrences.stream().filter(r -> selected.contains(r.getPublicId())).toList();
        }
        if (occurrences.isEmpty()) {
            throw ApiException.badRequest("RECURRING_009", "No upcoming occurrences left to book on this series.");
        }

        String batchId = UUID.randomUUID().toString();
        List<BookingResponse> booked = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (Ride ride : occurrences) {
            try {
                BookingCreateRequest bookingRequest = new BookingCreateRequest(
                        ride.getPublicId(), request.pickupLat(), request.pickupLng(), request.pickupAddress(),
                        request.dropLat(), request.dropLng(), request.dropAddress(), request.seats());
                booked.add(bookingService.createBooking(passengerUserPublicId, bookingRequest, batchId));
            } catch (ApiException e) {
                failures.add(ride.getDepartureDate() + ": " + e.getMessage());
            }
        }

        if (booked.isEmpty()) {
            throw ApiException.businessRule("RECURRING_010", "Could not book any occurrence: " + String.join("; ", failures));
        }

        return new RecurringBookingSummary(occurrences.size(), booked.size(), failures.size(), booked, failures);
    }

    /**
     * Accepts/rejects every booking in a batch (see bookAll) at once - the driver's one-click
     * response to "book all upcoming occurrences". Reuses BookingService.respondToBooking per
     * item (same ownership check, same PENDING-only guard), so one item failing doesn't block
     * the rest.
     */
    public BookingBatchSummary respondToBatch(String driverUserPublicId, String bookingBatchId, boolean accept) {
        List<Booking> batch = bookingRepository.findByBookingBatchIdOrderByCreatedAtAsc(bookingBatchId);
        if (batch.isEmpty()) {
            throw ApiException.notFound("RECURRING_011", "Booking batch not found.");
        }

        List<BookingResponse> responded = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (Booking booking : batch) {
            try {
                responded.add(bookingService.respondToBooking(driverUserPublicId, booking.getPublicId(), accept));
            } catch (ApiException e) {
                failures.add(booking.getRide().getDepartureDate() + ": " + e.getMessage());
            }
        }

        if (responded.isEmpty()) {
            throw ApiException.forbidden("RECURRING_012", "Could not respond to any booking in this batch: " + String.join("; ", failures));
        }

        return new BookingBatchSummary(batch.size(), responded.size(), failures.size(), responded, failures);
    }

    private List<LocalDate> occurrenceDates(LocalDate start, LocalDate end, List<DayOfWeek> daysOfWeek) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (daysOfWeek.contains(date.getDayOfWeek())) {
                dates.add(date);
            }
        }
        return dates;
    }

    private RecurringRideResponse toResponse(RecurringRide series) {
        List<Ride> occurrences = rideRepository.findByRecurringRideId(series.getId());
        long upcoming = occurrences.stream()
                .filter(r -> r.getStatus() == RideStatus.PENDING || r.getStatus() == RideStatus.ACTIVE)
                .count();
        List<String> days = Arrays.asList(series.getDaysOfWeek().split(","));
        return new RecurringRideResponse(
                series.getPublicId(),
                series.getOriginAddress(),
                series.getDestinationAddress(),
                days,
                series.getDepartureTime(),
                series.getStartDate(),
                series.getEndDate(),
                series.getTotalSeats(),
                series.getPricePerSeat(),
                series.getStatus().name(),
                occurrences.size(),
                (int) upcoming
        );
    }
}

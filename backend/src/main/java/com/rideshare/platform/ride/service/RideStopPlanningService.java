package com.rideshare.platform.ride.service;

import com.rideshare.platform.booking.entity.Booking;
import com.rideshare.platform.booking.entity.BookingStatus;
import com.rideshare.platform.booking.repository.BookingRepository;
import com.rideshare.platform.common.GeoUtils;
import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.location.dto.LocationResponse;
import com.rideshare.platform.location.service.RideLocationService;
import com.rideshare.platform.ride.dto.RideStopPlanResponse;
import com.rideshare.platform.ride.dto.RideStopResponse;
import com.rideshare.platform.ride.entity.Ride;
import com.rideshare.platform.ride.repository.RideRepository;
import com.rideshare.platform.route.service.RideStopPlanner;
import com.rideshare.platform.route.service.RideStopPlanner.Stop;
import com.rideshare.platform.user.entity.User;
import com.rideshare.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * FR: Pickup Ordering / Drop-off Ordering. Computes and persists, once per ride start, the
 * order the driver should visit confirmed passengers' pickup points, then their drop-off
 * points - reordering both away from booking order to approximately minimize total distance
 * (see RideStopPlanner), while keeping each passenger's own detour within the ride's
 * maxDetourKm.
 */
@Service
@RequiredArgsConstructor
public class RideStopPlanningService {

    private static final double METERS_PER_KM = 1000.0;
    private static final double DEFAULT_MAX_DETOUR_KM = 5.0;

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final RideLocationService rideLocationService;
    private final RideStopPlanner planner;

    /** Called once, from RideService.start(): computes the pickup order from the driver's
     *  current position (live GPS if already reported, else the ride's published origin), then
     *  the drop-off order from the last pickup, and persists both onto each booking. */
    @Transactional
    public void planAndPersist(Ride ride, String driverUserPublicId) {
        List<Booking> confirmed = confirmedBookings(ride);
        if (confirmed.isEmpty()) {
            return;
        }

        double[] start = startPoint(ride, driverUserPublicId);
        Map<String, Booking> byPublicId = confirmed.stream()
                .collect(Collectors.toMap(Booking::getPublicId, b -> b));

        List<Stop> pickupOrder = planner.plan(start[0], start[1],
                confirmed.stream().map(b -> new Stop(b.getPublicId(), b.getPickupLat(), b.getPickupLng())).toList());
        for (int i = 0; i < pickupOrder.size(); i++) {
            byPublicId.get(pickupOrder.get(i).key()).setPickupStopOrder(i + 1);
        }

        Stop lastPickup = pickupOrder.get(pickupOrder.size() - 1);

        // "May reorder drop-offs if it doesn't exceed the max permitted detour" - the constraint
        // is checked on every candidate swap during optimization (not just the final result), so
        // one passenger's tight detour limit only blocks the specific reorderings that would
        // violate it, rather than discarding optimization for every other passenger too. Dropping
        // off in the same order passengers were picked up is the always-available fallback if
        // even the initial nearest-neighbor tour can't clear the constraint.
        List<Stop> mirroredDropOrder = pickupOrder.stream()
                .map(p -> new Stop(p.key(), byPublicId.get(p.key()).getDropLat(), byPublicId.get(p.key()).getDropLng()))
                .toList();
        List<Stop> dropOrder = planner.planConstrained(lastPickup.lat(), lastPickup.lng(), mirroredDropOrder,
                detourConstraint(ride, byPublicId, start, pickupOrder));

        for (int i = 0; i < dropOrder.size(); i++) {
            byPublicId.get(dropOrder.get(i).key()).setDropStopOrder(i + 1);
        }

        bookingRepository.saveAll(confirmed);
    }

    @Transactional(readOnly = true)
    public RideStopPlanResponse getStopPlan(String userPublicId, String ridePublicId) {
        Ride ride = authorize(userPublicId, ridePublicId);
        double[] start = startPoint(ride, ride.getDriver().getUser().getPublicId());

        List<Booking> withPickupOrder = confirmedBookings(ride).stream()
                .filter(b -> b.getPickupStopOrder() != null)
                .sorted((a, b) -> a.getPickupStopOrder().compareTo(b.getPickupStopOrder()))
                .toList();
        List<Booking> withDropOrder = confirmedBookings(ride).stream()
                .filter(b -> b.getDropStopOrder() != null)
                .sorted((a, b) -> a.getDropStopOrder().compareTo(b.getDropStopOrder()))
                .toList();

        List<RideStopResponse> pickups = toResponses(withPickupOrder, Booking::getPickupStopOrder,
                Booking::getPickupLat, Booking::getPickupLng, Booking::getPickupAddress, start[0], start[1]);

        double lastPickupLat = withPickupOrder.isEmpty() ? start[0] : withPickupOrder.get(withPickupOrder.size() - 1).getPickupLat();
        double lastPickupLng = withPickupOrder.isEmpty() ? start[1] : withPickupOrder.get(withPickupOrder.size() - 1).getPickupLng();
        List<RideStopResponse> dropoffs = toResponses(withDropOrder, Booking::getDropStopOrder,
                Booking::getDropLat, Booking::getDropLng, Booking::getDropAddress, lastPickupLat, lastPickupLng);

        return new RideStopPlanResponse(start[0], start[1], pickups, dropoffs,
                ride.getDestinationLat(), ride.getDestinationLng(), ride.getDestinationAddress());
    }

    private interface DoubleGetter { double get(Booking b); }
    private interface StringGetter { String get(Booking b); }
    private interface OrderGetter { Integer get(Booking b); }

    private List<RideStopResponse> toResponses(List<Booking> ordered, OrderGetter order, DoubleGetter lat, DoubleGetter lng,
                                                StringGetter address, double startLat, double startLng) {
        List<RideStopResponse> responses = new ArrayList<>(ordered.size());
        double curLat = startLat;
        double curLng = startLng;
        for (Booking b : ordered) {
            double legMeters = GeoUtils.haversineMeters(curLat, curLng, lat.get(b), lng.get(b));
            responses.add(new RideStopResponse(b.getPublicId(), b.getPassenger().getName(), order.get(b),
                    lat.get(b), lng.get(b), address.get(b), legMeters / METERS_PER_KM));
            curLat = lat.get(b);
            curLng = lng.get(b);
        }
        return responses;
    }

    /** Builds a reusable "does this drop-off order keep everyone under the ride's max detour"
     *  check: the pickup side (fixed once the pickup order is set) is walked once up front, so
     *  each candidate drop order evaluated during 2-opt only costs an O(n) walk of the drop side,
     *  not a full re-walk of pickups too. Detour = actual distance travelled between a
     *  passenger's own pickup and drop, minus the direct (as-the-crow-flies) distance between
     *  those same two points - i.e. the extra distance *this* passenger incurs from sharing the
     *  ride, not the trip's total length. */
    private Predicate<List<Stop>> detourConstraint(Ride ride, Map<String, Booking> byPublicId,
                                                     double[] start, List<Stop> pickupOrder) {
        double maxDetourMeters = (ride.getMaxDetourKm() == null ? DEFAULT_MAX_DETOUR_KM : ride.getMaxDetourKm().doubleValue()) * METERS_PER_KM;

        Map<String, Double> cumulativeAtPickup = new HashMap<>();
        double cumulative = 0;
        double curLat = start[0];
        double curLng = start[1];
        for (Stop s : pickupOrder) {
            cumulative += GeoUtils.haversineMeters(curLat, curLng, s.lat(), s.lng());
            cumulativeAtPickup.put(s.key(), cumulative);
            curLat = s.lat();
            curLng = s.lng();
        }
        double pickupEndLat = curLat;
        double pickupEndLng = curLng;
        double pickupEndCumulative = cumulative;

        return dropOrder -> {
            double runningCumulative = pickupEndCumulative;
            double legLat = pickupEndLat;
            double legLng = pickupEndLng;
            for (Stop s : dropOrder) {
                runningCumulative += GeoUtils.haversineMeters(legLat, legLng, s.lat(), s.lng());
                Booking b = byPublicId.get(s.key());
                double actual = runningCumulative - cumulativeAtPickup.get(s.key());
                double direct = GeoUtils.haversineMeters(b.getPickupLat(), b.getPickupLng(), b.getDropLat(), b.getDropLng());
                if (actual - direct > maxDetourMeters) {
                    return false;
                }
                legLat = s.lat();
                legLng = s.lng();
            }
            return true;
        };
    }

    private double[] startPoint(Ride ride, String driverUserPublicId) {
        LocationResponse live = rideLocationService.get(driverUserPublicId, ride.getPublicId());
        return live != null ? new double[]{live.lat(), live.lng()} : new double[]{ride.getOriginLat(), ride.getOriginLng()};
    }

    private List<Booking> confirmedBookings(Ride ride) {
        return bookingRepository.findByRideId(ride.getId()).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .toList();
    }

    private Ride authorize(String userPublicId, String ridePublicId) {
        Ride ride = rideRepository.findByPublicId(ridePublicId)
                .orElseThrow(() -> ApiException.notFound("RIDE_006", "Ride not found."));
        boolean isDriver = ride.getDriver().getUser().getPublicId().equals(userPublicId);
        if (!isDriver) {
            User user = userRepository.findByPublicId(userPublicId)
                    .orElseThrow(() -> ApiException.notFound("USER_001", "User not found."));
            boolean isPassenger = bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(
                    ride.getId(), user.getId(), List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));
            if (!isPassenger) {
                throw ApiException.forbidden("RIDE_013", "Not authorized to view this ride's stop plan.");
            }
        }
        return ride;
    }
}

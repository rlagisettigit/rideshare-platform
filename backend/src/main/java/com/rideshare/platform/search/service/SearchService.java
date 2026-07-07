package com.rideshare.platform.search.service;

import com.rideshare.platform.common.GeoUtils;
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
import com.rideshare.platform.route.service.H3SpatialService;
import com.rideshare.platform.search.dto.RideSearchRequest;
import com.rideshare.platform.search.dto.RideSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FR: Section 8 Ride Search.
 * Search conditions: ride ACTIVE, seats available, departure within configurable window,
 * pickup/drop plausibly along the driver's route. Ranked per Section 8 Search Ranking.
 *
 * Route matching is delegated to H3SpatialService, which tolerates the fact that routes
 * are locally-interpolated straight lines rather than real road polylines: it accepts a
 * pickup/drop near either endpoint (same city/metro) or anywhere within a generous lateral
 * deviation of the endpoint-to-endpoint line, so a passenger searching "Hyderabad -> Nagole"
 * can match a driver publishing "Kukatpally -> Warangal".
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private final H3Service h3Service;
    private final H3SpatialService h3SpatialService;
    private final RideRepository rideRepository;
    private final RideRoutePointRepository rideRoutePointRepository;
    private final RecurringRideRepository recurringRideRepository;
    private final RideFareEstimator rideFareEstimator;

    @Value("${app.search.departure-window-hours:6}")
    private int departureWindowHours;

    @Transactional(readOnly = true)
    public List<RideSearchResult> search(RideSearchRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = request.travelDate().atStartOfDay();
        LocalDateTime windowEnd = windowStart.plusDays(1).plusHours(departureWindowHours);

        List<RideSearchResult> results = new ArrayList<>();

        // Rides are matched by coordinates already stored at publish time (see H3SpatialService) -
        // no live routing API call happens here. Scanning ACTIVE rides directly (rather than a
        // tight H3-cell shortlist) is what makes the generous city-to-city tolerance possible.
        for (Ride ride : rideRepository.findByStatus(RideStatus.ACTIVE)) {
            if (ride.getAvailableSeats() < request.passengers()) continue;
            if (ride.getDepartureAt().isBefore(now)) continue;
            if (ride.getDepartureAt().isBefore(windowStart) || ride.getDepartureAt().isAfter(windowEnd)) continue;

            List<RideRoutePoint> points = rideRoutePointRepository.findByRideIdOrderBySequenceNoAsc(ride.getId());
            if (points.isEmpty()) continue;

            List<String> routeIndices = points.stream().map(RideRoutePoint::getH3Cell).toList();
            H3SpatialService.RouteMatchResult match = h3SpatialService.matchPassengerToRoute(
                    request.pickupLat(), request.pickupLng(), request.dropLat(), request.dropLng(),
                    routeIndices,
                    ride.getOriginLat(), ride.getOriginLng(), ride.getDestinationLat(), ride.getDestinationLng());
            if (!match.isMatches()) continue;

            RideRoutePoint nearestPickup = nearestPoint(points, request.pickupLat(), request.pickupLng());
            RideRoutePoint nearestDrop = nearestPoint(points, request.dropLat(), request.dropLng());
            double detourKm = estimateDetourKm(nearestPickup, nearestDrop);

            // What a booking for this pickup/drop/seat-count will actually charge - not the
            // driver's advertised pricePerSeat - via the same PricingEngine BookingService uses.
            FareBreakdown fareBreakdown = rideFareEstimator.estimate(ride, nearestPickup, nearestDrop, request.passengers());

            // Lets the frontend offer "book all upcoming occurrences" when this result is one
            // date out of a driver's recurring series, instead of only this single date.
            String recurringRidePublicId = ride.getRecurringRideId() == null ? null
                    : recurringRideRepository.findById(ride.getRecurringRideId()).map(r -> r.getPublicId()).orElse(null);

            results.add(new RideSearchResult(
                    ride.getPublicId(),
                    ride.getDriver().getUser().getName(),
                    ride.getDriver().getUser().getAverageRating() == null ? null : ride.getDriver().getUser().getAverageRating().doubleValue(),
                    ride.getVehicle().getModel(),
                    ride.getDepartureAt(),
                    ride.getAvailableSeats(),
                    ride.getPricePerSeat(),
                    fareBreakdown.passengerFare(),
                    FareBreakdownResponse.DISCLAIMER,
                    match.getPickupDistanceFromStart() / 1000.0,
                    detourKm,
                    nearestPickup.getSequenceNo(),
                    nearestDrop.getSequenceNo(),
                    recurringRidePublicId
            ));
        }

        return rank(dedupeRecurringOccurrences(results), request.sortBy());
    }

    /** The departure window spans past midnight (see windowEnd above) to catch late-night
     *  departures, but that means a recurring series with an early-morning departure time can
     *  have two of its occurrences (today's and tomorrow's) both fall inside one search's
     *  window - same driver/route/price, just a different date. The UI already represents a
     *  series as a single card with a "book upcoming dates" picker, so collapse those down to
     *  the single soonest-departing occurrence per series instead of showing it twice. */
    private List<RideSearchResult> dedupeRecurringOccurrences(List<RideSearchResult> results) {
        Map<String, RideSearchResult> earliestPerSeries = new LinkedHashMap<>();
        List<RideSearchResult> deduped = new ArrayList<>();
        for (RideSearchResult result : results) {
            if (result.recurringRidePublicId() == null) {
                deduped.add(result);
                continue;
            }
            earliestPerSeries.merge(result.recurringRidePublicId(), result,
                    (a, b) -> a.departureAt().isBefore(b.departureAt()) ? a : b);
        }
        deduped.addAll(earliestPerSeries.values());
        return deduped;
    }

    private RideRoutePoint nearestPoint(List<RideRoutePoint> points, double lat, double lng) {
        String targetCell = h3Service.latLngToCell(lat, lng);
        return points.stream()
                .min(Comparator.comparingDouble(p -> h3Service.gridDistanceKm(targetCell, p.getH3Cell())))
                .orElse(null);
    }

    /** Detour proxy: distance along route between pickup and drop, vs. their direct distance. */
    private double estimateDetourKm(RideRoutePoint pickup, RideRoutePoint drop) {
        int onRouteMeters = Math.max(0, drop.getCumulativeDistanceM() - pickup.getCumulativeDistanceM());
        double directMeters = GeoUtils.haversineMeters(pickup.getLat(), pickup.getLng(), drop.getLat(), drop.getLng());
        return Math.max(0, onRouteMeters - directMeters) / 1000.0;
    }

    private List<RideSearchResult> rank(List<RideSearchResult> results, RideSearchRequest.SortOption sortBy) {
        Comparator<RideSearchResult> comparator = switch (sortBy == null ? RideSearchRequest.SortOption.EARLIEST_DEPARTURE : sortBy) {
            case NEAREST_PICKUP -> Comparator.comparingDouble(RideSearchResult::pickupDistanceKm);
            case LOWEST_DETOUR -> Comparator.comparingDouble(RideSearchResult::detourKm);
            case EARLIEST_DEPARTURE -> Comparator.comparing(RideSearchResult::departureAt);
            case DRIVER_RATING -> Comparator.comparing(RideSearchResult::driverRating,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case RIDE_PRICE -> Comparator.comparing(RideSearchResult::estimatedFare);
        };
        return results.stream().sorted(comparator).collect(Collectors.toList());
    }
}

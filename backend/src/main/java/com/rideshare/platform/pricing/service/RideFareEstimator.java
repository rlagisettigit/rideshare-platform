package com.rideshare.platform.pricing.service;

import com.rideshare.platform.ride.entity.Ride;
import com.rideshare.platform.route.entity.RideRoutePoint;
import com.rideshare.platform.route.repository.RideRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Builds a {@link FareContext} from a ride + route segment and runs it through the
 * {@link PricingEngine} - the one place this happens, so BookingService (actual booking fare),
 * SearchService (price shown on search results) and RideService (price shown on a ride's
 * details) all quote the SAME number instead of some of them falling back to the driver's raw
 * {@code pricePerSeat}.
 */
@Service
@RequiredArgsConstructor
public class RideFareEstimator {

    /** Fallback average speed (km/h) used to estimate segment duration when the ride's total route duration is unknown. */
    private static final BigDecimal DEFAULT_AVG_SPEED_KMPH = BigDecimal.valueOf(40);

    private final RideRouteRepository rideRouteRepository;
    private final PricingEngine pricingEngine;

    public FareBreakdown estimate(Ride ride, RideRoutePoint pickup, RideRoutePoint drop, int seats) {
        int segmentMeters = Math.max(0, drop.getCumulativeDistanceM() - pickup.getCumulativeDistanceM());
        BigDecimal distanceKm = BigDecimal.valueOf(segmentMeters).divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
        BigDecimal durationMinutes = estimateSegmentDurationMinutes(ride, segmentMeters, distanceKm);

        // Demand proxy: how full the ride already is (0-100), since there's no live demand feed.
        BigDecimal demandRatio = BigDecimal.valueOf(ride.getTotalSeats() - ride.getAvailableSeats())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(ride.getTotalSeats()), 4, RoundingMode.HALF_UP);

        FareContext context = new FareContext(ride.getVehicle().getCategory(), distanceKm, durationMinutes, seats,
                ride.getVehicle().getSeatingCapacity(), ride.getDepartureAt(), demandRatio, null);
        return pricingEngine.calculate(context);
    }

    private BigDecimal estimateSegmentDurationMinutes(Ride ride, int segmentMeters, BigDecimal distanceKm) {
        return rideRouteRepository.findByRideId(ride.getId())
                .filter(r -> r.getDistanceMeters() != null && r.getDistanceMeters() > 0 && r.getDurationSeconds() != null)
                .map(r -> BigDecimal.valueOf(segmentMeters)
                        .divide(BigDecimal.valueOf(r.getDistanceMeters()), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(r.getDurationSeconds()))
                        .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP))
                .orElseGet(() -> distanceKm.divide(DEFAULT_AVG_SPEED_KMPH, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(60)));
    }
}

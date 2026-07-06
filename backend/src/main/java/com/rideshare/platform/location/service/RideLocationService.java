package com.rideshare.platform.location.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideshare.platform.booking.entity.BookingStatus;
import com.rideshare.platform.booking.repository.BookingRepository;
import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.location.dto.LocationResponse;
import com.rideshare.platform.location.dto.LocationUpdateRequest;
import com.rideshare.platform.location.dto.RideRouteResponse;
import com.rideshare.platform.location.dto.RoutePointResponse;
import com.rideshare.platform.ride.entity.Ride;
import com.rideshare.platform.ride.entity.RideStatus;
import com.rideshare.platform.ride.repository.RideRepository;
import com.rideshare.platform.route.entity.RideRoute;
import com.rideshare.platform.route.repository.RideRouteRepository;
import com.rideshare.platform.route.repository.RideRoutePointRepository;
import com.rideshare.platform.user.entity.User;
import com.rideshare.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Live driver-location tracking, backed by Redis instead of a DB table: only the latest position
 * ever matters and pings arrive every few seconds, so a short-TTL cache entry (no history table
 * to grow forever) is the natural fit - keeps this feature free of any new persistent storage
 * and avoids per-update writes hitting MySQL.
 */
@Service
@RequiredArgsConstructor
public class RideLocationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RideRoutePointRepository rideRoutePointRepository;
    private final RideRouteRepository rideRouteRepository;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "ride_location:";
    private static final long TTL_MINUTES = 3;

    @Transactional(readOnly = true)
    public void publish(String driverUserPublicId, String ridePublicId, LocationUpdateRequest request) {
        Ride ride = rideRepository.findByPublicId(ridePublicId)
                .orElseThrow(() -> ApiException.notFound("LOCATION_001", "Ride not found."));
        if (!ride.getDriver().getUser().getPublicId().equals(driverUserPublicId)) {
            throw ApiException.forbidden("LOCATION_002", "You do not own this ride.");
        }
        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw ApiException.businessRule("LOCATION_003", "Location updates are only accepted while the ride is in progress.");
        }
        LocationResponse location = new LocationResponse(request.lat(), request.lng(), request.heading(), LocalDateTime.now());
        redisTemplate.opsForValue().set(KEY_PREFIX + ridePublicId, location, TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Transactional(readOnly = true)
    public LocationResponse get(String userPublicId, String ridePublicId) {
        authorize(userPublicId, ridePublicId);

        // The shared RedisTemplate's GenericJackson2JsonRedisSerializer was built from a plain
        // ObjectMapper (no default typing), so it writes/reads plain JSON with no embedded type
        // hint - get() comes back as a LinkedHashMap rather than a LocationResponse. Converting
        // explicitly avoids a ClassCastException on every read.
        Object raw = redisTemplate.opsForValue().get(KEY_PREFIX + ridePublicId);
        return raw == null ? null : objectMapper.convertValue(raw, LocationResponse.class);
    }

    /** The ride's full stored route, so the passenger's map can draw the pickup-to-destination line. */
    @Transactional(readOnly = true)
    public RideRouteResponse getRoute(String userPublicId, String ridePublicId) {
        Ride ride = authorize(userPublicId, ridePublicId);
        List<RoutePointResponse> points = rideRoutePointRepository.findByRideIdOrderBySequenceNoAsc(ride.getId())
                .stream().map(p -> new RoutePointResponse(p.getLat(), p.getLng())).toList();
        int distanceMeters = rideRouteRepository.findByRideId(ride.getId())
                .map(RideRoute::getDistanceMeters).orElse(0);
        return new RideRouteResponse(points, distanceMeters);
    }

    private Ride authorize(String userPublicId, String ridePublicId) {
        Ride ride = rideRepository.findByPublicId(ridePublicId)
                .orElseThrow(() -> ApiException.notFound("LOCATION_001", "Ride not found."));

        boolean isDriver = ride.getDriver().getUser().getPublicId().equals(userPublicId);
        if (!isDriver) {
            User user = userRepository.findByPublicId(userPublicId)
                    .orElseThrow(() -> ApiException.notFound("USER_001", "User not found."));
            boolean isPassenger = bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(
                    ride.getId(), user.getId(), List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));
            if (!isPassenger) {
                throw ApiException.forbidden("LOCATION_002", "Not authorized to view this ride's location.");
            }
        }
        return ride;
    }
}

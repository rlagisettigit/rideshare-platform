package com.rideshare.platform.route.service;

import com.rideshare.platform.common.GeoUtils;
import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.route.PolylineCodec;
import com.rideshare.platform.route.dto.RouteOption;
import com.rideshare.platform.route.entity.RideRoute;
import com.rideshare.platform.route.entity.RideRoutePoint;
import com.rideshare.platform.route.provider.RoutePoint;
import com.rideshare.platform.route.provider.RouteResult;
import com.rideshare.platform.route.provider.RoutingProvider;
import com.rideshare.platform.route.repository.RideRoutePointRepository;
import com.rideshare.platform.route.repository.RideRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * FR: Section 7 Route Management pipeline, executed when a driver publishes a ride:
 *   Call Routing Provider -> Receive Polyline -> Decode Polyline
 *   -> Convert to H3 Cells -> Store Ordered Route -> Build Redis Index
 */
@Service
@RequiredArgsConstructor
public class RouteService {

    /** A real road-following polyline can carry thousands of vertices for a long trip -
     *  storing/indexing every one of them makes publish slow for no search-accuracy benefit,
     *  since search tolerance already operates on a multi-km scale. Downsample to a cap. */
    private static final int MAX_ROUTE_POINTS = 300;

    private final RoutingProvider routingProvider;
    private final RoutePreviewService routePreviewService;
    private final H3Service h3Service;
    private final RideRouteRepository rideRouteRepository;
    private final RideRoutePointRepository rideRoutePointRepository;
    private final RouteRedisIndexService redisIndexService;

    /**
     * Tries a real, road-following Mappls route first (one call - the same cost as the
     * driver-facing preview button) so a ride only falls back to the local straight-line
     * interpolation when Mappls itself is unavailable, rather than whenever a driver simply
     * didn't click "Preview route options" before publishing.
     */
    @Transactional
    public List<RideRoutePoint> generateAndStoreRoute(Long rideId, double originLat, double originLng,
                                                        double destLat, double destLng) {
        return storeRoute(rideId, resolveRoute(originLat, originLng, destLat, destLng));
    }

    /**
     * Resolves a route for an origin/destination pair without persisting it yet - exposed so a
     * recurring ride series can fetch the identical route once and reuse the result across every
     * generated occurrence via {@link #storeRoute}, instead of one Mappls call per occurrence.
     * Never throws: falls back to the local straight-line interpolation if Mappls is unavailable.
     */
    public RouteResult resolveRoute(double originLat, double originLng, double destLat, double destLng) {
        Optional<RouteOption> auto = routePreviewService.fastestRouteSafe(originLat, originLng, destLat, destLng);
        if (auto.isPresent()) {
            RouteOption r = auto.get();
            List<double[]> decoded = PolylineCodec.decode(r.encodedPolyline());
            // Mappls only covers India: for a route entirely outside its coverage (e.g. a US
            // address) it can return a "successful" response that's actually degenerate - a
            // couple of vertices with zero reported distance/duration - rather than an error.
            // Storing that as-is would leave a ride with just its origin and destination as
            // "route points", which breaks any booking whose pickup/drop isn't one of those
            // two exact coordinates (BookingService's nearestPoint() has nothing else to snap
            // to). Treat that the same as Mappls being unavailable and fall through to the
            // local straight-line interpolation, which is geometry-only and always produces a
            // usable multi-point route regardless of where on Earth the ride is.
            boolean usable = decoded.size() >= 3 && r.distanceMeters() > 0;
            if (usable) {
                return toRouteResult("MAPPLS", r.encodedPolyline(), r.distanceMeters(), r.durationSeconds(), decoded);
            }
        }

        RouteResult result = routingProvider.getRoute(originLat, originLng, destLat, destLng);
        if (result == null || result.decodedPoints() == null || result.decodedPoints().isEmpty()) {
            throw ApiException.externalService("ROUTE_001", "Routing provider failed to generate a route.");
        }
        return result;
    }

    /**
     * Stores the specific route a driver picked from the publish-time route preview
     * (see RoutePreviewService) instead of the default locally-interpolated straight line -
     * this is the real, road-following Mappls polyline, which makes search matching for
     * mid-route passengers more accurate than the straight-line approximation.
     */
    @Transactional
    public List<RideRoutePoint> storeSelectedRoute(Long rideId, String provider, String encodedPolyline,
                                                    int distanceMeters, int durationSeconds) {
        List<double[]> decoded = PolylineCodec.decode(encodedPolyline);
        if (decoded.isEmpty()) {
            throw ApiException.badRequest("ROUTE_002", "Selected route polyline could not be decoded.");
        }
        return storeRoute(rideId, toRouteResult(provider, encodedPolyline, distanceMeters, durationSeconds, decoded));
    }

    private RouteResult toRouteResult(String provider, String encodedPolyline, int distanceMeters,
                                       int durationSeconds, List<double[]> decoded) {
        List<RoutePoint> points = new ArrayList<>();
        double cumulative = 0;
        for (int i = 0; i < decoded.size(); i++) {
            double lat = decoded.get(i)[0];
            double lng = decoded.get(i)[1];
            if (i > 0) {
                double[] prev = decoded.get(i - 1);
                cumulative += GeoUtils.haversineMeters(prev[0], prev[1], lat, lng);
            }
            points.add(new RoutePoint(lat, lng, (int) cumulative));
        }
        return new RouteResult(provider, encodedPolyline, distanceMeters, durationSeconds, points);
    }

    /** Persists an already-resolved route against a specific ride - reusable across many rides
     *  that share the same underlying route (see resolveRoute) without re-fetching or re-decoding. */
    @Transactional
    public List<RideRoutePoint> storeRoute(Long rideId, RouteResult result) {
        RideRoute route = new RideRoute();
        route.setRideId(rideId);
        route.setProvider(result.provider());
        route.setEncodedPolyline(result.encodedPolyline());
        route.setDistanceMeters(result.distanceMeters());
        route.setDurationSeconds(result.durationSeconds());
        rideRouteRepository.save(route);

        List<RoutePoint> sampled = downsample(result.decodedPoints());

        List<RideRoutePoint> points = new ArrayList<>(sampled.size());
        Set<String> h3Cells = new LinkedHashSet<>();
        int seq = 0;
        for (RoutePoint rp : sampled) {
            RideRoutePoint point = new RideRoutePoint();
            point.setRideId(rideId);
            point.setSequenceNo(seq++);
            point.setLat(rp.lat());
            point.setLng(rp.lng());
            point.setCumulativeDistanceM(rp.cumulativeDistanceMeters());
            String cell = h3Service.latLngToCell(rp.lat(), rp.lng());
            point.setH3Cell(cell);
            h3Cells.add(cell);
            points.add(point);
        }
        rideRoutePointRepository.saveAll(points);

        // Build Redis Index: H3 Cell -> Ride IDs, deduped and pipelined into one round trip.
        redisIndexService.indexCells(h3Cells, rideId);

        return points;
    }

    /** Uniformly samples down to MAX_ROUTE_POINTS, always keeping the first and last point. */
    private List<RoutePoint> downsample(List<RoutePoint> points) {
        if (points.size() <= MAX_ROUTE_POINTS) {
            return points;
        }
        List<RoutePoint> sampled = new ArrayList<>(MAX_ROUTE_POINTS);
        int lastIndex = points.size() - 1;
        for (int i = 0; i < MAX_ROUTE_POINTS; i++) {
            int index = (int) Math.round((double) i / (MAX_ROUTE_POINTS - 1) * lastIndex);
            sampled.add(points.get(index));
        }
        return sampled;
    }

    public List<RideRoutePoint> getRoutePoints(Long rideId) {
        return rideRoutePointRepository.findByRideIdOrderBySequenceNoAsc(rideId);
    }

    @Transactional
    public void removeRouteIndex(Long rideId) {
        List<RideRoutePoint> points = rideRoutePointRepository.findByRideIdOrderBySequenceNoAsc(rideId);
        Set<String> h3Cells = new LinkedHashSet<>();
        points.forEach(p -> h3Cells.add(p.getH3Cell()));
        redisIndexService.removeCellsForRide(h3Cells, rideId);
    }
}

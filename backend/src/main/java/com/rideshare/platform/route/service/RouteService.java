package com.rideshare.platform.route.service;

import com.rideshare.platform.common.GeoUtils;
import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.route.PolylineCodec;
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
import java.util.Set;

/**
 * FR: Section 7 Route Management pipeline, executed when a driver publishes a ride:
 *   Call Routing Provider -> Receive Polyline -> Decode Polyline
 *   -> Convert to H3 Cells -> Store Ordered Route -> Store H3 Index Map -> Build Redis Index
 */
@Service
@RequiredArgsConstructor
public class RouteService {

    /** A real road-following polyline can carry thousands of vertices for a long trip -
     *  storing/indexing every one of them makes publish slow for no search-accuracy benefit,
     *  since search tolerance already operates on a multi-km scale. Downsample to a cap. */
    private static final int MAX_ROUTE_POINTS = 300;

    private final RoutingProvider routingProvider;
    private final H3Service h3Service;
    private final RideRouteRepository rideRouteRepository;
    private final RideRoutePointRepository rideRoutePointRepository;
    private final RouteRedisIndexService redisIndexService;

    @Transactional
    public List<RideRoutePoint> generateAndStoreRoute(Long rideId, double originLat, double originLng,
                                                        double destLat, double destLng) {
        RouteResult result = routingProvider.getRoute(originLat, originLng, destLat, destLng);
        if (result == null || result.decodedPoints() == null || result.decodedPoints().isEmpty()) {
            throw ApiException.externalService("ROUTE_001", "Routing provider failed to generate a route.");
        }
        return storeRoute(rideId, result);
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

        RouteResult result = new RouteResult(provider, encodedPolyline, distanceMeters, durationSeconds, points);
        return storeRoute(rideId, result);
    }

    private List<RideRoutePoint> storeRoute(Long rideId, RouteResult result) {
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

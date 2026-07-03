package com.rideshare.platform.route.provider;

import com.rideshare.platform.common.GeoUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Default RoutingProvider implementation.
 *
 * Routes are computed locally from the origin/destination coordinates captured at publish
 * time - no live third-party routing API call is made. A route is approximated as a
 * densely interpolated straight line (one point roughly every 250m, mirroring
 * H3SpatialService's corridor sampling) between origin and destination; ride search then
 * matches passengers against this using H3SpatialService's tolerant corridor/deviation
 * checks rather than requiring an exact road-following polyline.
 *
 * A real routing vendor (Mappls, Google, OSRM, ...) can be plugged in behind this same
 * RoutingProvider interface later without touching RouteService / RideService.
 */
@Component
public class MapplsRoutingProvider implements RoutingProvider {

    private static final double METERS_PER_POINT = 250;
    private static final int MIN_POINTS = 20;

    @Override
    public RouteResult getRoute(double originLat, double originLng, double destLat, double destLng) {
        List<RoutePoint> points = new ArrayList<>();
        double totalDistance = GeoUtils.haversineMeters(originLat, originLng, destLat, destLng);
        int segments = Math.max(MIN_POINTS, (int) (totalDistance / METERS_PER_POINT));
        double cumulative = 0;
        for (int i = 0; i <= segments; i++) {
            double f = (double) i / segments;
            double lat = originLat + (destLat - originLat) * f;
            double lng = originLng + (destLng - originLng) * f;
            if (i > 0) {
                RoutePoint prev = points.get(i - 1);
                cumulative += GeoUtils.haversineMeters(prev.lat(), prev.lng(), lat, lng);
            }
            points.add(new RoutePoint(lat, lng, (int) cumulative));
        }
        return new RouteResult(providerName(), null, (int) totalDistance,
                (int) (totalDistance / 13.9), points); // ~50km/h average speed assumption
    }

    @Override
    public String providerName() {
        return "LOCAL_INTERPOLATED";
    }
}

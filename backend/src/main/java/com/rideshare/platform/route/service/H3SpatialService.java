package com.rideshare.platform.route.service;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * H3 Spatial Indexing Service for ride discovery.
 * Uses Uber's H3 hexagonal hierarchical spatial index to efficiently match
 * passenger pickup/dropoff locations with driver routes.
 */
@Service
public class H3SpatialService {

    protected final Log LOGGER = LogFactory.getLog(getClass());
    
    private final H3Core h3;
    
    // Resolution levels for H3 indexing
    // Resolution 9: ~0.5 km hexagon edge length - good for route segments
    // Resolution 8: ~1.2 km hexagon edge length - good for broad search
    private static final int ROUTE_RESOLUTION = 9;
    private static final int SEARCH_RESOLUTION = 8;
    
    // Maximum distance in meters to consider a location "on route"
    private static final double MAX_DISTANCE_TO_ROUTE_METERS = 2000; // 2 km tolerance

    public H3SpatialService() {
        try {
            this.h3 = H3Core.newInstance();
            LOGGER.info("✅ H3Core initialized successfully");
        } catch (IOException e) {
            LOGGER.error("❌ Failed to initialize H3Core", e);
            throw new RuntimeException("Failed to initialize H3 spatial indexing", e);
        }
    }

    /**
     * Convert latitude/longitude to H3 index at route resolution.
     */
    public String latLngToH3(double latitude, double longitude) {
        return h3.latLngToCellAddress(latitude, longitude, ROUTE_RESOLUTION);
    }

    /**
     * Convert H3 index back to latitude/longitude.
     */
    public LatLng h3ToLatLng(String h3Index) {
        return h3.cellToLatLng(h3Index);
    }

    /**
     * Generate H3 indices for an entire route by interpolating between start and end points.
     * This creates a "corridor" of hexagons representing the route.
     */
    public List<String> generateRouteIndices(double startLat, double startLng, 
                                             double endLat, double endLng) {
        Set<String> indices = new LinkedHashSet<>();
        
        // Add start and end points
        indices.add(latLngToH3(startLat, startLng));
        indices.add(latLngToH3(endLat, endLng));
        
        // Interpolate points along the route MORE DENSELY
        // Calculate number of intermediate points based on distance
        double distance = calculateDistance(startLat, startLng, endLat, endLng);
        int numPoints = Math.max(20, (int) (distance / 250)); // Point every 250 meters (denser sampling)
        
        for (int i = 1; i < numPoints; i++) {
            double ratio = (double) i / numPoints;
            double interpLat = startLat + (endLat - startLat) * ratio;
            double interpLng = startLng + (endLng - startLng) * ratio;
            indices.add(latLngToH3(interpLat, interpLng));
        }
        
        // Add k-ring (neighboring hexagons) to create a WIDER corridor
        Set<String> expandedIndices = new LinkedHashSet<>();
        for (String index : indices) {
            expandedIndices.add(index);
            // Add neighbors with k=2 for wider route corridor (~3-4 km width)
            // This helps catch cities slightly off the straight-line path
            expandedIndices.addAll(h3.gridDisk(index, 2));
        }
        
        LOGGER.debug("Generated " + expandedIndices.size() + " H3 indices for route");
        return new ArrayList<>(expandedIndices);
    }

    /**
     * Generate H3 indices for multiple waypoints (future enhancement for complex routes).
     */
    public List<String> generateRouteIndicesMultiplePoints(List<double[]> waypoints) {
        Set<String> allIndices = new LinkedHashSet<>();
        
        for (int i = 0; i < waypoints.size() - 1; i++) {
            double[] start = waypoints.get(i);
            double[] end = waypoints.get(i + 1);
            allIndices.addAll(generateRouteIndices(start[0], start[1], end[0], end[1]));
        }
        
        return new ArrayList<>(allIndices);
    }

    /**
     * Check if a passenger's pickup and dropoff locations are on the driver's route.
     * Returns true if both locations are within the route corridor and in correct order.
     * 
     * Enhanced matching:
     * - Pickup can be near route START or anywhere on route
     * - Dropoff can be near route END or anywhere on route
     * - This handles cases like: Driver "Kukatpally→Tirupati", Passenger "Hyderabad→Tirupati"
     */
    public RouteMatchResult matchPassengerToRoute(
            double pickupLat, double pickupLng,
            double dropoffLat, double dropoffLng,
            List<String> routeIndices,
            double routeStartLat, double routeStartLng,
            double routeEndLat, double routeEndLng) {
        
        LOGGER.debug("🔍 Matching passenger route to driver route:");
        LOGGER.debug("   Passenger: [" + pickupLat + "," + pickupLng + "] → [" + dropoffLat + "," + dropoffLng + "]");
        LOGGER.debug("   Driver: [" + routeStartLat + "," + routeStartLng + "] → [" + routeEndLat + "," + routeEndLng + "]");
        
        // Calculate distances to route endpoints
        double pickupToRouteStart = calculateDistance(pickupLat, pickupLng, routeStartLat, routeStartLng);
        double pickupToRouteEnd = calculateDistance(pickupLat, pickupLng, routeEndLat, routeEndLng);
        double dropoffToRouteStart = calculateDistance(dropoffLat, dropoffLng, routeStartLat, routeStartLng);
        double dropoffToRouteEnd = calculateDistance(dropoffLat, dropoffLng, routeEndLat, routeEndLng);
        
        // Flexible pickup matching: pickup can be near route start OR on route OR between start and end
        boolean pickupValid = false;
        String pickupReason = "";
        
        // Check if pickup is near route start (within 30 km - same city/metro area)
        if (pickupToRouteStart <= 30000) { // 30 km tolerance for city boundaries
            pickupValid = true;
            pickupReason = "near route start";
        } else {
            // Check if pickup is on route corridor (H3 match)
            String pickupH3 = latLngToH3(pickupLat, pickupLng);
            if (isLocationOnRoute(pickupH3, pickupLat, pickupLng, routeIndices)) {
                pickupValid = true;
                pickupReason = "on route corridor";
            } else {
                // Check if pickup is between start and end (mid-route city)
                double routeLength = calculateDistance(routeStartLat, routeStartLng, routeEndLat, routeEndLng);
                double distViaPickup = pickupToRouteStart + pickupToRouteEnd;
                double pickupDeviation = distViaPickup - routeLength;
                
                // Allow up to 100 km lateral deviation for mid-route pickups (increased tolerance)
                if (pickupDeviation <= 100000) {
                    pickupValid = true;
                    pickupReason = "mid-route (lateral deviation: " + Math.round(pickupDeviation/1000) + " km)";
                }
            }
        }
        
        if (!pickupValid) {
            return new RouteMatchResult(false, 
                "Pickup too far from route (nearest: " + Math.round(pickupToRouteStart/1000) + " km from start)", 
                0, 0);
        }
        
        // Flexible dropoff matching: dropoff can be near route end OR on route OR between start and end
        boolean dropoffValid = false;
        String dropoffReason = "";
        
        LOGGER.debug("🎯 Dropoff validation: Guntur [" + dropoffLat + "," + dropoffLng + "]");
        LOGGER.debug("   Distance to route start: " + Math.round(dropoffToRouteStart/1000) + " km");
        LOGGER.debug("   Distance to route end: " + Math.round(dropoffToRouteEnd/1000) + " km");
        
        // Check if dropoff is near route end (within 30 km)
        if (dropoffToRouteEnd <= 30000) {
            dropoffValid = true;
            dropoffReason = "near route end";
            LOGGER.debug("   ✅ Dropoff near route end");
        } else {
            // Check if dropoff is on route corridor (H3 match)
            String dropoffH3 = latLngToH3(dropoffLat, dropoffLng);
            if (isLocationOnRoute(dropoffH3, dropoffLat, dropoffLng, routeIndices)) {
                dropoffValid = true;
                dropoffReason = "on route corridor";
                LOGGER.debug("   ✅ Dropoff in H3 corridor");
            } else {
                LOGGER.debug("   ❌ Dropoff not in H3 corridor, checking triangle inequality...");
                // Check if dropoff is between start and end (mid-route city)
                // This handles cities along the route that might not be in H3 corridor
                // due to straight-line interpolation vs actual road path
                double routeLength = calculateDistance(routeStartLat, routeStartLng, routeEndLat, routeEndLng);
                
                // Dropoff should be closer to start than to end for first half, or vice versa
                // Use triangle inequality to check if point is roughly along the path
                double distViaDropoff = dropoffToRouteStart + dropoffToRouteEnd;
                double deviation = distViaDropoff - routeLength;
                
                LOGGER.debug("   Route length: " + Math.round(routeLength/1000) + " km");
                LOGGER.debug("   Via dropoff: " + Math.round(distViaDropoff/1000) + " km");
                LOGGER.debug("   Deviation: " + Math.round(deviation/1000) + " km");
                
                // Allow up to 100 km lateral deviation from straight line (increased tolerance)
                // This accounts for major road curves and city locations off the direct path
                // Highways can deviate 10-15% from straight lines in real-world scenarios
                if (deviation <= 100000) { // 100 km lateral tolerance for mid-route cities
                    dropoffValid = true;
                    dropoffReason = "mid-route (lateral deviation: " + Math.round(deviation/1000) + " km)";
                    LOGGER.debug("   ✅ Dropoff is mid-route (deviation within 100 km)");
                } else {
                    LOGGER.debug("   ❌ Dropoff deviation too large: " + Math.round(deviation/1000) + " km > 100 km");
                }
            }
        }
        
        if (!dropoffValid) {
            return new RouteMatchResult(false, 
                "Dropoff too far from route (nearest: " + Math.round(dropoffToRouteEnd/1000) + " km from end)", 
                0, 0);
        }
        
        // Validate order: pickup should be closer to route start than to route end
        // This ensures we're traveling in the correct direction
        if (pickupToRouteStart > pickupToRouteEnd) {
            return new RouteMatchResult(false, 
                "Invalid order: pickup closer to route end than start", 0, 0);
        }
        
        // Validate: dropoff should be farther from start than pickup
        if (dropoffToRouteStart <= pickupToRouteStart) {
            return new RouteMatchResult(false, 
                "Invalid order: dropoff before pickup in travel direction", 0, 0);
        }
        
        // Calculate segment distance
        double segmentDistance = calculateDistance(pickupLat, pickupLng, dropoffLat, dropoffLng);
        
        return new RouteMatchResult(true, 
            "Match: pickup " + pickupReason + ", dropoff " + dropoffReason, 
            pickupToRouteStart, segmentDistance);
    }

    /**
     * Check if a location (H3 index) is on or near the route.
     */
    private boolean isLocationOnRoute(String locationH3, double lat, double lng, 
                                     List<String> routeIndices) {
        // Direct match
        if (routeIndices.contains(locationH3)) {
            return true;
        }
        
        // Check neighbors (k=2 ring for wider tolerance)
        // This helps catch cities that are close to but not directly on the route
        Set<String> neighbors = new HashSet<>(h3.gridDisk(locationH3, 2));
        for (String neighbor : neighbors) {
            if (routeIndices.contains(neighbor)) {
                return true;
            }
        }
        
        // Fallback: check minimum distance to any route point (more expensive)
        // This handles edge cases where H3 hexagon boundaries might miss close points
        for (String routeH3 : routeIndices) {
            LatLng routePoint = h3ToLatLng(routeH3);
            double distance = calculateDistance(lat, lng, routePoint.lat, routePoint.lng);
            if (distance <= MAX_DISTANCE_TO_ROUTE_METERS) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Calculate distance between two points using Haversine formula (in meters).
     */
    public double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS = 6371000; // meters
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }

    /**
     * Get H3 indices at search resolution for a location.
     * Used to find nearby rides efficiently.
     */
    public Set<String> getSearchIndices(double latitude, double longitude, int kRing) {
        String centerH3 = h3.latLngToCellAddress(latitude, longitude, SEARCH_RESOLUTION);
        return new HashSet<>(h3.gridDisk(centerH3, kRing));
    }

    /**
     * Result object for route matching.
     */
    public static class RouteMatchResult {
        private final boolean matches;
        private final String reason;
        private final double pickupDistanceFromStart;
        private final double segmentDistance;

        public RouteMatchResult(boolean matches, String reason, 
                               double pickupDistanceFromStart, double segmentDistance) {
            this.matches = matches;
            this.reason = reason;
            this.pickupDistanceFromStart = pickupDistanceFromStart;
            this.segmentDistance = segmentDistance;
        }

        public boolean isMatches() { return matches; }
        public String getReason() { return reason; }
        public double getPickupDistanceFromStart() { return pickupDistanceFromStart; }
        public double getSegmentDistance() { return segmentDistance; }
    }
}

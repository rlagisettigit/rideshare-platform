package com.rideshare.platform.route.provider;

/**
 * Map Provider Abstraction (Section: Technology Stack - "Mappls Maps Provider Abstraction").
 * Concrete implementations (Mappls, Google, OSRM) plug in behind this interface so the
 * routing vendor can be swapped without touching RouteService / RideService.
 */
public interface RoutingProvider {
    RouteResult getRoute(double originLat, double originLng, double destLat, double destLng);
    String providerName();
}

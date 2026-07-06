package com.rideshare.platform.location.dto;

import java.util.List;

public record RideRouteResponse(List<RoutePointResponse> points, int distanceMeters) {}

package com.rideshare.platform.route.provider;

import java.util.List;

/** Provider-agnostic routing result. FR: Section 7 - "Call Routing Provider -> Receive Polyline". */
public record RouteResult(
        String provider,
        String encodedPolyline,
        int distanceMeters,
        int durationSeconds,
        List<RoutePoint> decodedPoints
) {}

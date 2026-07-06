package com.rideshare.platform.ride.dto;

import java.util.List;

/** FR: Pickup Ordering / Drop-off Ordering - the driver's optimized visiting order for this
 *  ride's confirmed passengers, computed once when the ride is started. */
public record RideStopPlanResponse(
        double startLat,
        double startLng,
        List<RideStopResponse> pickups,
        List<RideStopResponse> dropoffs,
        double destLat,
        double destLng,
        String destAddress
) {}

package com.rideshare.platform.ride.dto;

/** One stop in the driver's optimized pickup or drop-off order for an in-progress ride. */
public record RideStopResponse(
        String bookingPublicId,
        String passengerName,
        int order,
        double lat,
        double lng,
        String address,
        double legDistanceKm
) {}

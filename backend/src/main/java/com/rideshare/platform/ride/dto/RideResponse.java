package com.rideshare.platform.ride.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code pricePerSeat} is the driver's advertised listing price; {@code estimatedFarePerSeat} is
 * the PricingEngine's computed price for one seat over the ride's full origin-to-destination
 * route (a passenger's actual pickup/drop will usually cover less distance and cost less - see
 * search results or POST /bookings/preview for a pickup/drop-specific quote). {@code fareDisclaimer}
 * is the caveat to display alongside estimatedFarePerSeat.
 */
public record RideResponse(
        String publicId,
        String driverName,
        Double driverRating,
        String vehicleModel,
        String originAddress,
        String destinationAddress,
        LocalDateTime departureAt,
        Integer availableSeats,
        BigDecimal pricePerSeat,
        BigDecimal estimatedFarePerSeat,
        String fareDisclaimer,
        boolean womenOnly,
        boolean petsAllowed,
        boolean luggageAllowed,
        String status,
        String recurringRidePublicId
) {}

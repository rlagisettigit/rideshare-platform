package com.rideshare.platform.ride.entity;

import com.rideshare.platform.common.BaseEntity;
import com.rideshare.platform.driver.entity.Driver;
import com.rideshare.platform.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/** FR: Section 6 Ride Management - Create Ride. */
@Getter
@Setter
@Entity
@Table(name = "rides")
public class Ride extends BaseEntity {

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private String publicId = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "origin_address", nullable = false)
    private String originAddress;
    @Column(name = "origin_lat", nullable = false)
    private double originLat;
    @Column(name = "origin_lng", nullable = false)
    private double originLng;

    @Column(name = "destination_address", nullable = false)
    private String destinationAddress;
    @Column(name = "destination_lat", nullable = false)
    private double destinationLat;
    @Column(name = "destination_lng", nullable = false)
    private double destinationLng;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;
    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;
    @Column(name = "departure_at", nullable = false)
    private LocalDateTime departureAt;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;
    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Column(name = "price_per_seat", nullable = false)
    private BigDecimal pricePerSeat = BigDecimal.ZERO;

    @Column(name = "luggage_allowed", nullable = false)
    private boolean luggageAllowed = true;
    @Column(name = "smoking_allowed", nullable = false)
    private boolean smokingAllowed = false;
    @Column(name = "music_preference")
    private String musicPreference;
    @Column(name = "women_only", nullable = false)
    private boolean womenOnly = false;
    @Column(name = "pets_allowed", nullable = false)
    private boolean petsAllowed = false;

    private String description;

    @Column(name = "max_detour_km", nullable = false)
    private BigDecimal maxDetourKm = BigDecimal.valueOf(5);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status = RideStatus.PENDING;

    @Column(name = "route_generated", nullable = false)
    private boolean routeGenerated = false;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    /** Set when this ride was generated from a RecurringRide series; plain id rather than a
     *  relation, matching the lighter-weight "reference" pattern already used elsewhere
     *  (e.g. RideRoutePoint.rideId) instead of a full @ManyToOne back-reference. */
    @Column(name = "recurring_ride_id")
    private Long recurringRideId;
}

package com.rideshare.platform.ride.entity;

import com.rideshare.platform.common.BaseEntity;
import com.rideshare.platform.driver.entity.Driver;
import com.rideshare.platform.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A driver-defined repeating schedule (e.g. "every weekday at 9am") - not booked directly.
 * Creating one immediately generates an independently bookable {@link Ride} for every matching
 * date, reusing the exact same publish/search/booking pipeline as a one-off ride.
 */
@Getter
@Setter
@Entity
@Table(name = "recurring_rides")
public class RecurringRide extends BaseEntity {

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

    /** Comma-separated java.time.DayOfWeek names, e.g. "MONDAY,TUESDAY,WEDNESDAY". */
    @Column(name = "days_of_week", nullable = false)
    private String daysOfWeek;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "price_per_seat", nullable = false)
    private BigDecimal pricePerSeat = BigDecimal.ZERO;

    @Column(name = "luggage_allowed", nullable = false)
    private boolean luggageAllowed = true;
    @Column(name = "smoking_allowed", nullable = false)
    private boolean smokingAllowed = false;
    @Column(name = "women_only", nullable = false)
    private boolean womenOnly = false;
    @Column(name = "pets_allowed", nullable = false)
    private boolean petsAllowed = false;

    private String description;

    @Column(name = "max_detour_km", nullable = false)
    private BigDecimal maxDetourKm = BigDecimal.valueOf(5);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurringRideStatus status = RecurringRideStatus.ACTIVE;
}

package com.rideshare.platform.booking.entity;

import com.rideshare.platform.common.BaseEntity;
import com.rideshare.platform.ride.entity.Ride;
import com.rideshare.platform.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** FR: Section 9 Booking Management. */
@Getter
@Setter
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private String publicId = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @Column(name = "pickup_lat", nullable = false)
    private double pickupLat;
    @Column(name = "pickup_lng", nullable = false)
    private double pickupLng;
    @Column(name = "pickup_address")
    private String pickupAddress;
    @Column(name = "pickup_sequence_no", nullable = false)
    private int pickupSequenceNo;

    @Column(name = "drop_lat", nullable = false)
    private double dropLat;
    @Column(name = "drop_lng", nullable = false)
    private double dropLng;
    @Column(name = "drop_address")
    private String dropAddress;
    @Column(name = "drop_sequence_no", nullable = false)
    private int dropSequenceNo;

    @Column(name = "seats_booked", nullable = false)
    private int seatsBooked;

    @Column(nullable = false)
    private BigDecimal fare = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "cancellation_reason")
    private String cancellationReason;
}

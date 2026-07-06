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

    /** 1-based visiting order among this ride's confirmed passengers, computed by
     *  RideStopPlanner when the driver starts the ride - null until then. Distinct from
     *  pickup/dropSequenceNo above, which locate this booking along the ride's fixed route
     *  polyline rather than order it relative to other passengers' stops. */
    @Column(name = "pickup_stop_order")
    private Integer pickupStopOrder;

    @Column(name = "drop_stop_order")
    private Integer dropStopOrder;

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

    /** Set when this booking was created as part of "book all upcoming occurrences" on a
     *  recurring ride, so the driver can accept/reject the whole batch in one action. Null
     *  for a normal single-ride booking. */
    @Column(name = "booking_batch_id")
    private String bookingBatchId;
}

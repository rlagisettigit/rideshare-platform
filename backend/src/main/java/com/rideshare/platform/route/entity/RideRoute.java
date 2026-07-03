package com.rideshare.platform.route.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** FR: Section 7 Route Management - stores the decoded polyline metadata for a ride. */
@Getter
@Setter
@Entity
@Table(name = "ride_routes")
public class RideRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ride_id", nullable = false, unique = true)
    private Long rideId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "encoded_polyline", columnDefinition = "TEXT")
    private String encodedPolyline;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

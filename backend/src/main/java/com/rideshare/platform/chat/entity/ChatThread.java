package com.rideshare.platform.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** FR: Section 13 Chat - Passenger <-> Driver, one thread per (ride, passenger) pair. */
@Getter
@Setter
@Entity
@Table(name = "chat_threads")
public class ChatThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ride_id", nullable = false)
    private Long rideId;

    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // FR: "Blocked after ride expiration (configurable)"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

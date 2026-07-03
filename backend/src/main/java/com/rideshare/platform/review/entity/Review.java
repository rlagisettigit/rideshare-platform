package com.rideshare.platform.review.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** FR: Section 14 Reviews - Passenger rates Driver, Driver rates Passenger. */
@Getter
@Setter
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ride_id", nullable = false)
    private Long rideId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "reviewee_id", nullable = false)
    private Long revieweeId;

    @Column(nullable = false)
    private Integer rating; // 1-5

    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

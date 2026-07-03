package com.rideshare.platform.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** FR: Section 22 API Standards - "Idempotency keys for payment and booking operations." */
@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "INR";

    private String provider;

    @Column(name = "provider_ref")
    private String providerRef;

    @Column(nullable = false)
    private String status = "INITIATED"; // INITIATED, SUCCESS, FAILED, REFUNDED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

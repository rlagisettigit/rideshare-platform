package com.rideshare.platform.coupon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "discount_type", nullable = false)
    private String discountType; // FLAT, PERCENT

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "max_discount")
    private BigDecimal maxDiscount;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(nullable = false)
    private boolean active = true;
}

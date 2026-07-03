package com.rideshare.platform.driver.entity;

import com.rideshare.platform.common.BaseEntity;
import com.rideshare.platform.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** FR: Section 4 Driver Requirements. */
@Getter
@Setter
@Entity
@Table(name = "drivers")
public class Driver extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "license_number", nullable = false)
    private String licenseNumber;

    @Column(name = "license_doc_url")
    private String licenseDocUrl;

    @Column(name = "government_id_type")
    private String governmentIdType;

    @Column(name = "government_id_doc_url")
    private String governmentIdDocUrl;

    @Column(name = "address_proof_doc_url")
    private String addressProofDocUrl;

    @Column(name = "selfie_doc_url")
    private String selfieDocUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverStatus status = DriverStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "is_online", nullable = false)
    private boolean online = false;

    @Column(name = "last_online_at")
    private LocalDateTime lastOnlineAt;
}

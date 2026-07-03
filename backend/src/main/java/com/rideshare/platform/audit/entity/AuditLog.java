package com.rideshare.platform.audit.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** FR: Section 18 Audit - immutable audit records for every sensitive action. */
@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String action; // LOGIN, LOGOUT, RIDE_PUBLISH, BOOKING, PAYMENT, REFUND, ADMIN_ACTION, CONFIG_CHANGE

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details_json")
    private String detailsJson;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

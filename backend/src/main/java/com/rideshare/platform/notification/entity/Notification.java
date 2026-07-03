package com.rideshare.platform.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** FR: Section 12 Notification - Channels: Push, SMS, Email, In-App. */
@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String channel; // PUSH, SMS, EMAIL, IN_APP

    @Column(name = "event_type", nullable = false)
    private String eventType;

    /** Correlates related notifications about the same thing (e.g. a ride's public id), so a
     *  later event can find and remove an earlier one it supersedes. */
    @Column(name = "reference_id")
    private String referenceId;

    private String title;
    private String body;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

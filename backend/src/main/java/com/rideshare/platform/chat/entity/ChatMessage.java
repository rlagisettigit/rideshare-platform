package com.rideshare.platform.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** FR: Section 13 Chat features - Text, Images, Location, Ride Details, Read Receipts. */
@Getter
@Setter
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "message_type", nullable = false)
    private String messageType = "TEXT"; // TEXT, IMAGE, LOCATION, RIDE_DETAILS

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

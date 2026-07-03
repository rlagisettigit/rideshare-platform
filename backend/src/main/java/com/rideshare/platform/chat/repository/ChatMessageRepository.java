package com.rideshare.platform.chat.repository;

import com.rideshare.platform.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByThreadIdOrderByCreatedAtAsc(Long threadId);
}

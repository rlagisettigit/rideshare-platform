package com.rideshare.platform.chat.service;

import com.rideshare.platform.chat.entity.ChatMessage;
import com.rideshare.platform.chat.entity.ChatThread;
import com.rideshare.platform.chat.repository.ChatMessageRepository;
import com.rideshare.platform.chat.repository.ChatThreadRepository;
import com.rideshare.platform.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** FR: Section 13 Chat. */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatMessage sendMessage(Long rideId, Long passengerId, Long driverId, Long senderId,
                                    String messageType, String content) {
        ChatThread thread = chatThreadRepository.findByRideIdAndPassengerId(rideId, passengerId)
                .orElseGet(() -> {
                    ChatThread t = new ChatThread();
                    t.setRideId(rideId);
                    t.setPassengerId(passengerId);
                    t.setDriverId(driverId);
                    return chatThreadRepository.save(t);
                });

        if (thread.getExpiresAt() != null && thread.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.businessRule("CHAT_001", "This chat has expired and is no longer available.");
        }

        ChatMessage message = new ChatMessage();
        message.setThreadId(thread.getId());
        message.setSenderId(senderId);
        message.setMessageType(messageType);
        message.setContent(content);
        chatMessageRepository.save(message);

        messagingTemplate.convertAndSend("/topic/chat/" + thread.getId(), message);
        return message;
    }

    public List<ChatMessage> history(Long threadId) {
        return chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId);
    }
}

package com.rideshare.platform.chat.controller;

import com.rideshare.platform.chat.entity.ChatMessage;
import com.rideshare.platform.chat.service.ChatService;
import com.rideshare.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FR: Section 13 Chat - REST history endpoint; live messages flow over /ws/chat (STOMP). */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/threads/{threadId}/messages")
    public ApiResponse<List<ChatMessage>> history(@PathVariable Long threadId) {
        return ApiResponse.ok(chatService.history(threadId));
    }
}

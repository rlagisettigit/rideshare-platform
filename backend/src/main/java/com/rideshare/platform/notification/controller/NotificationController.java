package com.rideshare.platform.notification.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.notification.entity.Notification;
import com.rideshare.platform.notification.repository.NotificationRepository;
import com.rideshare.platform.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ApiResponse<List<Notification>> myNotifications(@AuthenticationPrincipal String userPublicId) {
        Long userId = userRepository.findByPublicId(userPublicId).orElseThrow().getId();
        return ApiResponse.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }
}

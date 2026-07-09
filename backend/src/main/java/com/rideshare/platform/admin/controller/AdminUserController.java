package com.rideshare.platform.admin.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.user.dto.UserProfileResponse;
import com.rideshare.platform.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR: Section 16 Admin Portal - Users module. Lets an existing admin grant ADMIN to another
 * user, so a second admin doesn't require touching ADMIN_BOOTSTRAP_EMAIL/PASSWORD env vars and
 * restarting (see AdminBootstrapRunner for how the first admin comes into existence).
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users")
public class AdminUserController {

    private final UserService userService;

    @PostMapping("/{userId}/promote")
    public ApiResponse<UserProfileResponse> promote(@PathVariable Long userId) {
        return ApiResponse.ok(userService.promoteToAdmin(userId), "User promoted to admin.");
    }
}

package com.rideshare.platform.user.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.user.dto.UpdateProfileRequest;
import com.rideshare.platform.user.dto.UserProfileResponse;
import com.rideshare.platform.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** FR-003 Profile Management. */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(@AuthenticationPrincipal String userPublicId) {
        return ApiResponse.ok(userService.getProfile(userPublicId));
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMyProfile(@AuthenticationPrincipal String userPublicId,
                                                              @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(userPublicId, request), "Profile updated.");
    }
}

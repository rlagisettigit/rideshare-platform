package com.rideshare.platform.driver.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.driver.dto.DriverOnboardRequest;
import com.rideshare.platform.driver.dto.DriverResponse;
import com.rideshare.platform.driver.service.DriverService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** FR: Section 4 Driver Requirements. */
@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Tag(name = "Drivers")
public class DriverController {

    private final DriverService driverService;

    @PostMapping("/onboard")
    public ApiResponse<DriverResponse> onboard(@AuthenticationPrincipal String userPublicId,
                                                @Valid @RequestBody DriverOnboardRequest request) {
        return ApiResponse.ok(driverService.onboard(userPublicId, request), "Driver application submitted for verification.");
    }

    @PostMapping("/availability/online")
    public ApiResponse<DriverResponse> goOnline(@AuthenticationPrincipal String userPublicId) {
        return ApiResponse.ok(driverService.setAvailability(userPublicId, true));
    }

    @PostMapping("/availability/offline")
    public ApiResponse<DriverResponse> goOffline(@AuthenticationPrincipal String userPublicId) {
        return ApiResponse.ok(driverService.setAvailability(userPublicId, false));
    }
}

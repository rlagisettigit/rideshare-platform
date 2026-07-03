package com.rideshare.platform.admin.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.driver.dto.DriverResponse;
import com.rideshare.platform.driver.service.DriverService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** FR: Section 16 Admin Portal - Drivers module (KYC review/approve/reject). */
@RestController
@RequestMapping("/api/v1/admin/drivers")
@RequiredArgsConstructor
@Tag(name = "Admin - Drivers")
public class AdminDriverController {

    private final DriverService driverService;

    public record ReviewRequest(boolean approve, String rejectionReason) {}

    @PostMapping("/{driverId}/review")
    public ApiResponse<DriverResponse> review(@PathVariable Long driverId, @RequestBody ReviewRequest request) {
        return ApiResponse.ok(driverService.review(driverId, request.approve(), request.rejectionReason()));
    }
}

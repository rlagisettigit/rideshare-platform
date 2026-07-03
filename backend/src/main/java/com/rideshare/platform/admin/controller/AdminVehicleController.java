package com.rideshare.platform.admin.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.vehicle.dto.VehicleResponse;
import com.rideshare.platform.vehicle.service.VehicleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** FR: Section 16 Admin Portal - Vehicles module. */
@RestController
@RequestMapping("/api/v1/admin/vehicles")
@RequiredArgsConstructor
@Tag(name = "Admin - Vehicles")
public class AdminVehicleController {

    private final VehicleService vehicleService;

    public record ReviewRequest(boolean approve) {}

    @PostMapping("/{vehicleId}/review")
    public ApiResponse<VehicleResponse> review(@PathVariable Long vehicleId, @RequestBody ReviewRequest request) {
        return ApiResponse.ok(vehicleService.review(vehicleId, request.approve()));
    }
}

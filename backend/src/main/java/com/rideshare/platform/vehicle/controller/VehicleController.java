package com.rideshare.platform.vehicle.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.vehicle.dto.VehicleRequest;
import com.rideshare.platform.vehicle.dto.VehicleResponse;
import com.rideshare.platform.vehicle.service.VehicleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FR: Section 5 Vehicle Requirements. */
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ApiResponse<VehicleResponse> register(@AuthenticationPrincipal String userPublicId,
                                                  @Valid @RequestBody VehicleRequest request) {
        return ApiResponse.ok(vehicleService.register(userPublicId, request), "Vehicle submitted for approval.");
    }

    @GetMapping("/me")
    public ApiResponse<List<VehicleResponse>> myVehicles(@AuthenticationPrincipal String userPublicId) {
        return ApiResponse.ok(vehicleService.myVehicles(userPublicId));
    }
}

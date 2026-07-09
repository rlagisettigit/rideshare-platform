package com.rideshare.platform.location.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.location.dto.LocationResponse;
import com.rideshare.platform.location.dto.LocationUpdateRequest;
import com.rideshare.platform.location.dto.RideRouteResponse;
import com.rideshare.platform.location.service.RideLocationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Live driver-location tracking and route progress while a ride is IN_PROGRESS. */
@RestController
@RequestMapping("/api/v1/rides/{ridePublicId}")
@RequiredArgsConstructor
@Tag(name = "Ride Location")
public class RideLocationController {

    private final RideLocationService rideLocationService;

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/location")
    public ApiResponse<Void> publish(@AuthenticationPrincipal String userPublicId,
                                      @PathVariable String ridePublicId,
                                      @Valid @RequestBody LocationUpdateRequest request) {
        rideLocationService.publish(userPublicId, ridePublicId, request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/location")
    public ApiResponse<LocationResponse> get(@AuthenticationPrincipal String userPublicId,
                                              @PathVariable String ridePublicId) {
        return ApiResponse.ok(rideLocationService.get(userPublicId, ridePublicId));
    }

    @GetMapping("/route")
    public ApiResponse<RideRouteResponse> route(@AuthenticationPrincipal String userPublicId,
                                                 @PathVariable String ridePublicId) {
        return ApiResponse.ok(rideLocationService.getRoute(userPublicId, ridePublicId));
    }
}

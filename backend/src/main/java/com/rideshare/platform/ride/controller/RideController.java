package com.rideshare.platform.ride.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.ride.dto.RideCreateRequest;
import com.rideshare.platform.ride.dto.RideResponse;
import com.rideshare.platform.ride.dto.RideStopPlanResponse;
import com.rideshare.platform.ride.service.RideService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FR: Section 6 Ride Management. */
@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
@Tag(name = "Rides")
public class RideController {

    private final RideService rideService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RideResponse> publish(@AuthenticationPrincipal String userPublicId,
                                              @Valid @RequestBody RideCreateRequest request) {
        return ApiResponse.ok(rideService.publish(userPublicId, request), "Ride published.");
    }

    @GetMapping("/{publicId}")
    public ApiResponse<RideResponse> get(@PathVariable String publicId) {
        return ApiResponse.ok(rideService.getByPublicId(publicId));
    }

    @GetMapping("/me")
    public ApiResponse<List<RideResponse>> myRides(@AuthenticationPrincipal String userPublicId) {
        return ApiResponse.ok(rideService.myRides(userPublicId));
    }

    @PostMapping("/{publicId}/cancel")
    public ApiResponse<RideResponse> cancel(@AuthenticationPrincipal String userPublicId,
                                             @PathVariable String publicId) {
        return ApiResponse.ok(rideService.cancel(userPublicId, publicId), "Ride cancelled.");
    }

    @PostMapping("/{publicId}/start")
    public ApiResponse<RideResponse> start(@AuthenticationPrincipal String userPublicId,
                                            @PathVariable String publicId) {
        return ApiResponse.ok(rideService.start(userPublicId, publicId), "Ride started.");
    }

    @PostMapping("/{publicId}/finish")
    public ApiResponse<RideResponse> finish(@AuthenticationPrincipal String userPublicId,
                                             @PathVariable String publicId) {
        return ApiResponse.ok(rideService.finish(userPublicId, publicId), "Ride completed.");
    }

    /** FR: Pickup/Drop-off Ordering - the optimized stop plan computed when the driver started
     *  the ride. Visible to the driver and to any of the ride's confirmed passengers. */
    @GetMapping("/{publicId}/stops")
    public ApiResponse<RideStopPlanResponse> stops(@AuthenticationPrincipal String userPublicId,
                                                    @PathVariable String publicId) {
        return ApiResponse.ok(rideService.getStopPlan(userPublicId, publicId));
    }
}

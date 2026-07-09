package com.rideshare.platform.ride.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.ride.dto.RecurringBookingRequest;
import com.rideshare.platform.ride.dto.RecurringBookingSummary;
import com.rideshare.platform.ride.dto.RecurringOccurrenceResponse;
import com.rideshare.platform.ride.dto.RecurringRideCreateRequest;
import com.rideshare.platform.ride.dto.RecurringRideResponse;
import com.rideshare.platform.ride.service.RecurringRideService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FR: recurring rides - drivers offering the same trip on a repeating schedule. */
@RestController
@RequestMapping("/api/v1/recurring-rides")
@RequiredArgsConstructor
@Tag(name = "Recurring Rides")
public class RecurringRideController {

    private final RecurringRideService recurringRideService;

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping
    public ApiResponse<RecurringRideResponse> create(@AuthenticationPrincipal String userPublicId,
                                                       @Valid @RequestBody RecurringRideCreateRequest request) {
        return ApiResponse.ok(recurringRideService.create(userPublicId, request), "Recurring ride created.");
    }

    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/me")
    public ApiResponse<List<RecurringRideResponse>> myRecurringRides(@AuthenticationPrincipal String userPublicId) {
        return ApiResponse.ok(recurringRideService.myRecurringRides(userPublicId));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{publicId}/cancel")
    public ApiResponse<RecurringRideResponse> cancel(@AuthenticationPrincipal String userPublicId, @PathVariable String publicId) {
        return ApiResponse.ok(recurringRideService.cancel(userPublicId, publicId), "Recurring ride cancelled.");
    }

    @GetMapping("/{publicId}/occurrences")
    public ApiResponse<List<RecurringOccurrenceResponse>> occurrences(@PathVariable String publicId) {
        return ApiResponse.ok(recurringRideService.getOccurrences(publicId));
    }

    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping("/{publicId}/book")
    public ApiResponse<RecurringBookingSummary> bookAll(@AuthenticationPrincipal String userPublicId,
                                                         @PathVariable String publicId,
                                                         @Valid @RequestBody RecurringBookingRequest request) {
        return ApiResponse.ok(recurringRideService.bookAll(userPublicId, publicId, request), "Booking requests sent for the selected occurrences.");
    }
}

package com.rideshare.platform.booking.controller;

import com.rideshare.platform.booking.dto.BookingBatchSummary;
import com.rideshare.platform.booking.dto.BookingCreateRequest;
import com.rideshare.platform.booking.dto.BookingResponse;
import com.rideshare.platform.booking.dto.CancelBookingRequest;
import com.rideshare.platform.booking.service.BookingService;
import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.pricing.dto.FareBreakdownResponse;
import com.rideshare.platform.ride.service.RecurringRideService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FR: Section 9 Booking Management. Idempotency-Key header recommended for POST (Section 22). */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings")
public class BookingController {

    private final BookingService bookingService;
    private final RecurringRideService recurringRideService;

    @PreAuthorize("hasRole('PASSENGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingResponse> create(@AuthenticationPrincipal String userPublicId,
                                                @Valid @RequestBody BookingCreateRequest request) {
        return ApiResponse.ok(bookingService.createBooking(userPublicId, request), "Booking requested.");
    }

    /** Lets a passenger see the price for their pickup/drop before they commit to booking - see the "disclaimer" field. */
    @PostMapping("/preview")
    public ApiResponse<FareBreakdownResponse> preview(@Valid @RequestBody BookingCreateRequest request) {
        return ApiResponse.ok(bookingService.previewFare(request));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{publicId}/accept")
    public ApiResponse<BookingResponse> accept(@AuthenticationPrincipal String driverPublicId,
                                                @PathVariable String publicId) {
        return ApiResponse.ok(bookingService.respondToBooking(driverPublicId, publicId, true), "Booking confirmed.");
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{publicId}/reject")
    public ApiResponse<BookingResponse> reject(@AuthenticationPrincipal String driverPublicId,
                                                @PathVariable String publicId) {
        return ApiResponse.ok(bookingService.respondToBooking(driverPublicId, publicId, false), "Booking rejected.");
    }

    /** Accepts/rejects every booking created together via "book all upcoming occurrences" in one action. */
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/batch/{batchId}/accept")
    public ApiResponse<BookingBatchSummary> acceptBatch(@AuthenticationPrincipal String driverPublicId,
                                                         @PathVariable String batchId) {
        return ApiResponse.ok(recurringRideService.respondToBatch(driverPublicId, batchId, true), "Batch confirmed.");
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/batch/{batchId}/reject")
    public ApiResponse<BookingBatchSummary> rejectBatch(@AuthenticationPrincipal String driverPublicId,
                                                         @PathVariable String batchId) {
        return ApiResponse.ok(recurringRideService.respondToBatch(driverPublicId, batchId, false), "Batch rejected.");
    }

    @PostMapping("/{publicId}/cancel")
    public ApiResponse<BookingResponse> cancel(@AuthenticationPrincipal String userPublicId,
                                                @PathVariable String publicId,
                                                @RequestBody(required = false) CancelBookingRequest request) {
        return ApiResponse.ok(bookingService.cancel(userPublicId, publicId, request), "Booking cancelled.");
    }

    @GetMapping("/me")
    public ApiResponse<List<BookingResponse>> myBookings(@AuthenticationPrincipal String userPublicId) {
        return ApiResponse.ok(bookingService.myBookings(userPublicId));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/driver")
    public ApiResponse<List<BookingResponse>> driverRequests(@AuthenticationPrincipal String driverUserPublicId) {
        return ApiResponse.ok(bookingService.driverBookingRequests(driverUserPublicId));
    }

    @GetMapping("/{publicId}")
    public ApiResponse<BookingResponse> get(@AuthenticationPrincipal String userPublicId,
                                             @PathVariable String publicId) {
        return ApiResponse.ok(bookingService.getByPublicId(userPublicId, publicId));
    }
}

package com.rideshare.platform.booking.controller;

import com.rideshare.platform.booking.dto.BookingCreateRequest;
import com.rideshare.platform.booking.dto.BookingResponse;
import com.rideshare.platform.booking.dto.CancelBookingRequest;
import com.rideshare.platform.booking.service.BookingService;
import com.rideshare.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingResponse> create(@AuthenticationPrincipal String userPublicId,
                                                @Valid @RequestBody BookingCreateRequest request) {
        return ApiResponse.ok(bookingService.createBooking(userPublicId, request), "Booking requested.");
    }

    @PostMapping("/{publicId}/accept")
    public ApiResponse<BookingResponse> accept(@AuthenticationPrincipal String driverPublicId,
                                                @PathVariable String publicId) {
        return ApiResponse.ok(bookingService.respondToBooking(driverPublicId, publicId, true), "Booking confirmed.");
    }

    @PostMapping("/{publicId}/reject")
    public ApiResponse<BookingResponse> reject(@AuthenticationPrincipal String driverPublicId,
                                                @PathVariable String publicId) {
        return ApiResponse.ok(bookingService.respondToBooking(driverPublicId, publicId, false), "Booking rejected.");
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

package com.rideshare.platform.review.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.review.dto.PendingReviewResponse;
import com.rideshare.platform.review.dto.ReviewResponse;
import com.rideshare.platform.review.entity.Review;
import com.rideshare.platform.review.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FR: Section 14 Reviews - passenger rates driver, driver rates passenger. */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public record ReviewRequest(String ridePublicId, String revieweeUserPublicId, int rating, String comment) {}

    @PostMapping
    public ApiResponse<Review> submit(@AuthenticationPrincipal String reviewerPublicId,
                                       @RequestBody ReviewRequest request) {
        return ApiResponse.ok(reviewService.submit(reviewerPublicId, request.ridePublicId(),
                request.revieweeUserPublicId(), request.rating(), request.comment()), "Review submitted.");
    }

    @GetMapping("/pending")
    public ApiResponse<List<PendingReviewResponse>> pending(@AuthenticationPrincipal String userPublicId) {
        return ApiResponse.ok(reviewService.pendingReviews(userPublicId));
    }

    @GetMapping("/received")
    public ApiResponse<List<ReviewResponse>> received(@AuthenticationPrincipal String userPublicId) {
        return ApiResponse.ok(reviewService.receivedReviews(userPublicId));
    }
}

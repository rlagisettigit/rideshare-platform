package com.rideshare.platform.review.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.review.entity.Review;
import com.rideshare.platform.review.service.ReviewService;
import com.rideshare.platform.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** FR: Section 14 Reviews. */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public record ReviewRequest(Long rideId, Long revieweeUserId, int rating, String comment) {}

    @PostMapping
    public ApiResponse<Review> submit(@AuthenticationPrincipal String reviewerPublicId,
                                       @RequestBody ReviewRequest request) {
        Long reviewerId = userRepository.findByPublicId(reviewerPublicId).orElseThrow().getId();
        return ApiResponse.ok(reviewService.submit(request.rideId(), reviewerId, request.revieweeUserId(),
                request.rating(), request.comment()), "Review submitted.");
    }
}

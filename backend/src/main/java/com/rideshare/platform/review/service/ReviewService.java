package com.rideshare.platform.review.service;

import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.review.entity.Review;
import com.rideshare.platform.review.repository.ReviewRepository;
import com.rideshare.platform.user.entity.User;
import com.rideshare.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** FR: Section 14 Reviews - rating 1-5, average recalculated asynchronously. */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Transactional
    public Review submit(Long rideId, Long reviewerId, Long revieweeId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw ApiException.badRequest("REVIEW_001", "Rating must be between 1 and 5.");
        }
        if (reviewRepository.existsByRideIdAndReviewerIdAndRevieweeId(rideId, reviewerId, revieweeId)) {
            throw ApiException.conflict("REVIEW_002", "You have already reviewed this user for this ride.");
        }
        Review review = new Review();
        review.setRideId(rideId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setRating(rating);
        review.setComment(comment);
        reviewRepository.save(review);

        recalculateAverageRating(revieweeId);
        return review;
    }

    @Async
    public void recalculateAverageRating(Long userId) {
        List<Review> reviews = reviewRepository.findByRevieweeId(userId);
        if (reviews.isEmpty()) return;
        BigDecimal avg = reviews.stream()
                .map(r -> BigDecimal.valueOf(r.getRating()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP);

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setAverageRating(avg);
            userRepository.save(user);
        }
    }
}

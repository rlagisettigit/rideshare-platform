package com.rideshare.platform.review.repository;

import com.rideshare.platform.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRevieweeId(Long revieweeId);
    boolean existsByRideIdAndReviewerIdAndRevieweeId(Long rideId, Long reviewerId, Long revieweeId);
}

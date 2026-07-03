package com.rideshare.platform.review.service;

import com.rideshare.platform.booking.entity.Booking;
import com.rideshare.platform.booking.entity.BookingStatus;
import com.rideshare.platform.booking.repository.BookingRepository;
import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.driver.entity.Driver;
import com.rideshare.platform.driver.repository.DriverRepository;
import com.rideshare.platform.review.dto.PendingReviewResponse;
import com.rideshare.platform.review.dto.ReviewResponse;
import com.rideshare.platform.review.entity.Review;
import com.rideshare.platform.review.repository.ReviewRepository;
import com.rideshare.platform.ride.entity.Ride;
import com.rideshare.platform.ride.entity.RideStatus;
import com.rideshare.platform.ride.repository.RideRepository;
import com.rideshare.platform.user.entity.User;
import com.rideshare.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * FR: Section 14 Reviews - both directions: passenger rates driver, driver rates passenger.
 * Rating 1-5, average recalculated asynchronously. A review is only allowed once the ride is
 * FINISHED and the reviewer/reviewee were actually connected by a COMPLETED booking on it -
 * "all possible ratings" means every such (reviewer, reviewee) pair the ride's completion
 * opened up, surfaced via pendingReviews().
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public Review submit(String reviewerUserPublicId, String ridePublicId, String revieweeUserPublicId,
                          int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw ApiException.badRequest("REVIEW_001", "Rating must be between 1 and 5.");
        }

        User reviewer = userRepository.findByPublicId(reviewerUserPublicId)
                .orElseThrow(() -> ApiException.notFound("USER_001", "User not found."));
        User reviewee = userRepository.findByPublicId(revieweeUserPublicId)
                .orElseThrow(() -> ApiException.notFound("USER_001", "Reviewee not found."));
        Ride ride = rideRepository.findByPublicId(ridePublicId)
                .orElseThrow(() -> ApiException.notFound("RIDE_006", "Ride not found."));

        if (ride.getStatus() != RideStatus.FINISHED) {
            throw ApiException.businessRule("REVIEW_003", "You can only rate a ride after it's completed.");
        }

        boolean reviewerIsRideDriver = ride.getDriver().getUser().getId().equals(reviewer.getId());
        if (reviewerIsRideDriver) {
            // Driver rating a passenger: reviewee must have a completed booking on this ride.
            boolean valid = bookingRepository.existsByRideIdAndPassengerIdAndStatus(
                    ride.getId(), reviewee.getId(), BookingStatus.COMPLETED);
            if (!valid) {
                throw ApiException.forbidden("REVIEW_004", "That passenger did not complete this ride with you.");
            }
        } else {
            // Passenger rating the driver: reviewer must have a completed booking, reviewee must be the driver.
            boolean reviewerCompletedThisRide = bookingRepository.existsByRideIdAndPassengerIdAndStatus(
                    ride.getId(), reviewer.getId(), BookingStatus.COMPLETED);
            boolean revieweeIsDriver = ride.getDriver().getUser().getId().equals(reviewee.getId());
            if (!reviewerCompletedThisRide || !revieweeIsDriver) {
                throw ApiException.forbidden("REVIEW_004", "You did not complete this ride with that person.");
            }
        }

        if (reviewRepository.existsByRideIdAndReviewerIdAndRevieweeId(ride.getId(), reviewer.getId(), reviewee.getId())) {
            throw ApiException.conflict("REVIEW_002", "You have already reviewed this user for this ride.");
        }

        Review review = new Review();
        review.setRideId(ride.getId());
        review.setReviewerId(reviewer.getId());
        review.setRevieweeId(reviewee.getId());
        review.setRating(rating);
        review.setComment(comment);
        reviewRepository.save(review);

        recalculateAverageRating(reviewee.getId());
        return review;
    }

    /** Every (ride, counterparty) pair the current user is eligible to rate but hasn't yet - both directions. */
    @Transactional(readOnly = true)
    public List<PendingReviewResponse> pendingReviews(String userPublicId) {
        User user = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> ApiException.notFound("USER_001", "User not found."));
        List<PendingReviewResponse> pending = new ArrayList<>();

        // As passenger: rate the driver of every ride with a completed booking.
        for (Booking booking : bookingRepository.findByPassengerIdOrderByCreatedAtDesc(user.getId())) {
            if (booking.getStatus() != BookingStatus.COMPLETED) continue;
            Ride ride = booking.getRide();
            User driverUser = ride.getDriver().getUser();
            if (!reviewRepository.existsByRideIdAndReviewerIdAndRevieweeId(ride.getId(), user.getId(), driverUser.getId())) {
                pending.add(new PendingReviewResponse(ride.getPublicId(), driverUser.getPublicId(), driverUser.getName(),
                        "RATE_DRIVER", ride.getOriginAddress(), ride.getDestinationAddress(), ride.getDepartureAt()));
            }
        }

        // As driver: rate each passenger with a completed booking on rides this user has published.
        Driver driver = driverRepository.findByUserId(user.getId()).orElse(null);
        if (driver != null) {
            for (Booking booking : bookingRepository.findByRideDriverIdOrderByCreatedAtDesc(driver.getId())) {
                if (booking.getStatus() != BookingStatus.COMPLETED) continue;
                Ride ride = booking.getRide();
                User passenger = booking.getPassenger();
                if (!reviewRepository.existsByRideIdAndReviewerIdAndRevieweeId(ride.getId(), user.getId(), passenger.getId())) {
                    pending.add(new PendingReviewResponse(ride.getPublicId(), passenger.getPublicId(), passenger.getName(),
                            "RATE_PASSENGER", ride.getOriginAddress(), ride.getDestinationAddress(), ride.getDepartureAt()));
                }
            }
        }

        return pending;
    }

    /** Reviews the current user has received, most recent first. */
    @Transactional(readOnly = true)
    public List<ReviewResponse> receivedReviews(String userPublicId) {
        User user = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> ApiException.notFound("USER_001", "User not found."));
        return reviewRepository.findByRevieweeId(user.getId()).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(r -> new ReviewResponse(r.getId(), reviewerName(r.getReviewerId()), r.getRating(),
                        r.getComment(), r.getCreatedAt()))
                .toList();
    }

    private String reviewerName(Long reviewerId) {
        return userRepository.findById(reviewerId).map(User::getName).orElse("Unknown");
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

package com.rideshare.platform.booking.repository;

import com.rideshare.platform.booking.entity.Booking;
import com.rideshare.platform.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByPublicId(String publicId);
    List<Booking> findByPassengerIdOrderByCreatedAtDesc(Long passengerId);
    List<Booking> findByRideId(Long rideId);

    /** Booking requests (any status) across every ride published by this driver. */
    @Query("select b from Booking b where b.ride.driver.id = :driverId order by b.createdAt desc")
    List<Booking> findByRideDriverIdOrderByCreatedAtDesc(Long driverId);

    /** FR: "No duplicate booking" - checks for an existing active booking by this passenger on this ride. */
    boolean existsByRideIdAndPassengerIdAndStatusIn(Long rideId, Long passengerId, List<BookingStatus> statuses);

    /** Used to confirm a driver<->passenger review is between two people who actually completed a ride together. */
    boolean existsByRideIdAndPassengerIdAndStatus(Long rideId, Long passengerId, BookingStatus status);
}

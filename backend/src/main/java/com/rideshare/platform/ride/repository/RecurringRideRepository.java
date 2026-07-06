package com.rideshare.platform.ride.repository;

import com.rideshare.platform.ride.entity.RecurringRide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecurringRideRepository extends JpaRepository<RecurringRide, Long> {
    Optional<RecurringRide> findByPublicId(String publicId);
    List<RecurringRide> findByDriverIdOrderByStartDateDesc(Long driverId);
}

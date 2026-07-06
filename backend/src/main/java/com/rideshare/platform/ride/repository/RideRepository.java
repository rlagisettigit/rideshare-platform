package com.rideshare.platform.ride.repository;

import com.rideshare.platform.ride.entity.Ride;
import com.rideshare.platform.ride.entity.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {
    Optional<Ride> findByPublicId(String publicId);
    List<Ride> findByDriverIdOrderByDepartureAtDesc(Long driverId);
    List<Ride> findByStatus(RideStatus status);
    boolean existsByVehicleId(Long vehicleId);
    List<Ride> findByRecurringRideId(Long recurringRideId);

    /** FR: Section 9 Seat Allocation - "Seats reserved atomically. No overselling." */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Ride r where r.id = :id")
    Optional<Ride> findByIdForUpdate(Long id);
}

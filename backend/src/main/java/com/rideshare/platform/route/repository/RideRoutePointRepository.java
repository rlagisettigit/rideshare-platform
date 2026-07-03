package com.rideshare.platform.route.repository;

import com.rideshare.platform.route.entity.RideRoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRoutePointRepository extends JpaRepository<RideRoutePoint, Long> {
    List<RideRoutePoint> findByRideIdOrderBySequenceNoAsc(Long rideId);
    void deleteByRideId(Long rideId);
}

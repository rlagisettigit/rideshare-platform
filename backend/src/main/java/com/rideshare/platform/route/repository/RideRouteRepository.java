package com.rideshare.platform.route.repository;

import com.rideshare.platform.route.entity.RideRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RideRouteRepository extends JpaRepository<RideRoute, Long> {
    Optional<RideRoute> findByRideId(Long rideId);
}

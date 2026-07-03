package com.rideshare.platform.vehicle.repository;

import com.rideshare.platform.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByDriverId(Long driverId);
    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);
    boolean existsByVehicleNumber(String vehicleNumber);
}

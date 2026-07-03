package com.rideshare.platform.vehicle.service;

import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.driver.entity.Driver;
import com.rideshare.platform.driver.repository.DriverRepository;
import com.rideshare.platform.vehicle.dto.VehicleRequest;
import com.rideshare.platform.vehicle.dto.VehicleResponse;
import com.rideshare.platform.vehicle.entity.Vehicle;
import com.rideshare.platform.vehicle.entity.VehicleStatus;
import com.rideshare.platform.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** FR: Section 5 Vehicle Requirements. */
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public VehicleResponse register(String userPublicId, VehicleRequest request) {
        if (vehicleRepository.existsByVehicleNumber(request.vehicleNumber())) {
            throw ApiException.conflict("VEHICLE_001", "Vehicle number already registered.");
        }
        Driver driver = driverRepository.findByUserPublicId(userPublicId)
                .orElseThrow(() -> ApiException.notFound("DRIVER_001", "Driver profile not found."));

        Vehicle vehicle = new Vehicle();
        vehicle.setDriver(driver);
        vehicle.setVehicleNumber(request.vehicleNumber());
        vehicle.setBrand(request.brand());
        vehicle.setModel(request.model());
        vehicle.setFuelType(request.fuelType());
        vehicle.setTransmission(request.transmission());
        vehicle.setColor(request.color());
        vehicle.setSeatingCapacity(request.seatingCapacity());
        vehicle.setInsuranceExpiry(request.insuranceExpiry());
        vehicle.setRegistrationExpiry(request.registrationExpiry());
        // TEMPORARY: admin review UI/role isn't built yet, so auto-approve vehicles at
        // registration to unblock ride publishing. Revert to VehicleStatus.PENDING once
        // the admin approval flow ships.
        vehicle.setStatus(VehicleStatus.APPROVED);

        vehicleRepository.save(vehicle);
        return toResponse(vehicle);
    }

    public List<VehicleResponse> myVehicles(String userPublicId) {
        Driver driver = driverRepository.findByUserPublicId(userPublicId)
                .orElseThrow(() -> ApiException.notFound("DRIVER_001", "Driver profile not found."));
        return vehicleRepository.findByDriverId(driver.getId()).stream().map(this::toResponse).toList();
    }

    /** Used by RideService: FR "Vehicle Verified" ride-publish validation. */
    public Vehicle getApprovedVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> ApiException.notFound("VEHICLE_002", "Vehicle not found."));
        if (vehicle.getStatus() != VehicleStatus.APPROVED) {
            throw ApiException.businessRule("VEHICLE_003", "Vehicle is not approved.");
        }
        return vehicle;
    }

    @Transactional
    public VehicleResponse review(Long vehicleId, boolean approve) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> ApiException.notFound("VEHICLE_002", "Vehicle not found."));
        vehicle.setStatus(approve ? VehicleStatus.APPROVED : VehicleStatus.REJECTED);
        vehicleRepository.save(vehicle);
        return toResponse(vehicle);
    }

    private VehicleResponse toResponse(Vehicle v) {
        return new VehicleResponse(v.getId(), v.getVehicleNumber(), v.getBrand(), v.getModel(),
                v.getSeatingCapacity(), v.getStatus().name());
    }
}

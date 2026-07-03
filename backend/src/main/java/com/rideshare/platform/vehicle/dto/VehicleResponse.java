package com.rideshare.platform.vehicle.dto;

public record VehicleResponse(
        Long id,
        String vehicleNumber,
        String brand,
        String model,
        Integer seatingCapacity,
        String status
) {}

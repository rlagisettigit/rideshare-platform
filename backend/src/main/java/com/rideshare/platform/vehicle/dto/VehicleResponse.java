package com.rideshare.platform.vehicle.dto;

import java.time.LocalDate;

public record VehicleResponse(
        Long id,
        String vehicleNumber,
        String brand,
        String model,
        String category,
        String fuelType,
        String transmission,
        String color,
        Integer seatingCapacity,
        LocalDate insuranceExpiry,
        LocalDate registrationExpiry,
        String status
) {}

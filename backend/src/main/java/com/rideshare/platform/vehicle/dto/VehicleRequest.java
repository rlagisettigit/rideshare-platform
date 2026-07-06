package com.rideshare.platform.vehicle.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record VehicleRequest(
        @NotBlank String vehicleNumber,
        String brand,
        String model,
        String category,
        String fuelType,
        String transmission,
        String color,
        @NotNull @Min(1) Integer seatingCapacity,
        @Future LocalDate insuranceExpiry,
        @Future LocalDate registrationExpiry
) {}

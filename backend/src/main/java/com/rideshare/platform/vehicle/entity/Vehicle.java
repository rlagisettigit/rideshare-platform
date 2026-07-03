package com.rideshare.platform.vehicle.entity;

import com.rideshare.platform.common.BaseEntity;
import com.rideshare.platform.driver.entity.Driver;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** FR: Section 5 Vehicle Requirements. */
@Getter
@Setter
@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "vehicle_number", nullable = false, unique = true)
    private String vehicleNumber;

    private String brand;
    private String model;

    @Column(name = "fuel_type")
    private String fuelType;

    private String transmission;
    private String color;

    @Column(name = "seating_capacity", nullable = false)
    private Integer seatingCapacity;

    @Column(name = "insurance_expiry")
    private LocalDate insuranceExpiry;

    @Column(name = "registration_expiry")
    private LocalDate registrationExpiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status = VehicleStatus.PENDING;
}

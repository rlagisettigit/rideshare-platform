package com.rideshare.platform.vehicle.entity;

import com.rideshare.platform.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A known make/model a driver can pick from when registering a vehicle, instead of free-typing brand/model. */
@Getter
@Setter
@Entity
@Table(name = "vehicle_catalog")
public class VehicleCatalogEntry extends BaseEntity {

    private String brand;
    private String model;
    private String category;
    private Integer seats;
}

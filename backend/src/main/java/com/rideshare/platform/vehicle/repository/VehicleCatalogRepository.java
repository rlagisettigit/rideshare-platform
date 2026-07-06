package com.rideshare.platform.vehicle.repository;

import com.rideshare.platform.vehicle.entity.VehicleCatalogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleCatalogRepository extends JpaRepository<VehicleCatalogEntry, Long> {
    List<VehicleCatalogEntry> findAllByOrderByBrandAscModelAsc();
}

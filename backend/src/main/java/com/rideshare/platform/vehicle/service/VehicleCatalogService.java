package com.rideshare.platform.vehicle.service;

import com.rideshare.platform.vehicle.dto.VehicleCatalogResponse;
import com.rideshare.platform.vehicle.entity.VehicleCatalogEntry;
import com.rideshare.platform.vehicle.repository.VehicleCatalogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The catalog is small, identical for every user, and only ever changes via a Flyway
 * migration - never at runtime through the app itself - so it's loaded into memory once at
 * startup rather than hitting MySQL (or even Redis, which would still cost a network hop)
 * on every "Register a vehicle" page load.
 */
@Service
@RequiredArgsConstructor
public class VehicleCatalogService {

    private final VehicleCatalogRepository vehicleCatalogRepository;

    private volatile List<VehicleCatalogResponse> cached = List.of();

    @PostConstruct
    void loadCatalog() {
        cached = vehicleCatalogRepository.findAllByOrderByBrandAscModelAsc()
                .stream().map(this::toResponse).toList();
    }

    public List<VehicleCatalogResponse> list() {
        return cached;
    }

    private VehicleCatalogResponse toResponse(VehicleCatalogEntry e) {
        return new VehicleCatalogResponse(e.getId(), e.getBrand(), e.getModel(), e.getCategory(), e.getSeats());
    }
}

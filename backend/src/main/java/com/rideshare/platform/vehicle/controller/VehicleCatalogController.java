package com.rideshare.platform.vehicle.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.vehicle.dto.VehicleCatalogResponse;
import com.rideshare.platform.vehicle.service.VehicleCatalogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Master list of known vehicle makes/models, used to populate the "Register a vehicle" dropdowns. */
@RestController
@RequestMapping("/api/v1/vehicle-catalog")
@RequiredArgsConstructor
@Tag(name = "Vehicle Catalog")
public class VehicleCatalogController {

    private final VehicleCatalogService vehicleCatalogService;

    @GetMapping
    public ApiResponse<List<VehicleCatalogResponse>> list() {
        return ApiResponse.ok(vehicleCatalogService.list());
    }
}

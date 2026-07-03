package com.rideshare.platform.route.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.route.dto.RouteOption;
import com.rideshare.platform.route.dto.RoutePlacesRequest;
import com.rideshare.platform.route.dto.RoutePreviewRequest;
import com.rideshare.platform.route.service.RoutePreviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FR: Section 6 Ride Management - route preview shown to a driver before publishing a ride. */
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@Tag(name = "Routes")
public class RouteController {

    private final RoutePreviewService routePreviewService;

    @PostMapping("/preview")
    public ApiResponse<List<RouteOption>> preview(@Valid @RequestBody RoutePreviewRequest request) {
        return ApiResponse.ok(routePreviewService.previewRoutes(
                request.originLat(), request.originLng(), request.destinationLat(), request.destinationLng()));
    }

    @PostMapping("/preview/places")
    public ApiResponse<List<String>> places(@Valid @RequestBody RoutePlacesRequest request) {
        return ApiResponse.ok(routePreviewService.placesForRoute(request.encodedPolyline()));
    }
}

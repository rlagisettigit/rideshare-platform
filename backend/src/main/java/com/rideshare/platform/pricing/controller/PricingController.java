package com.rideshare.platform.pricing.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.pricing.dto.FareBreakdownResponse;
import com.rideshare.platform.pricing.dto.FareEstimateRequest;
import com.rideshare.platform.pricing.service.FareContext;
import com.rideshare.platform.pricing.service.PricingEngine;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** Standalone fare quote: lets the frontend preview a price before a driver publishes a ride or a rider books one. */
@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
@Tag(name = "Pricing")
public class PricingController {

    /** Fallback average speed (km/h) used to estimate duration when the caller doesn't supply one. */
    private static final BigDecimal DEFAULT_AVG_SPEED_KMPH = BigDecimal.valueOf(40);

    private final PricingEngine pricingEngine;

    @PostMapping("/estimate")
    public ApiResponse<FareBreakdownResponse> estimate(@Valid @RequestBody FareEstimateRequest request) {
        BigDecimal durationMinutes = request.durationMinutes() != null
                ? request.durationMinutes()
                : request.distanceKm().divide(DEFAULT_AVG_SPEED_KMPH, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(60));

        FareContext context = new FareContext(request.vehicleCategory(), request.distanceKm(), durationMinutes,
                request.seats(), request.vehicleTotalSeats(), request.pickupAt(), request.demandRatio(), request.weatherCondition());

        return ApiResponse.ok(FareBreakdownResponse.from(pricingEngine.calculate(context)));
    }
}

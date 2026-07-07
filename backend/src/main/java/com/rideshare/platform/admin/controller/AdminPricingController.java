package com.rideshare.platform.admin.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.pricing.dto.PricingFactorRequest;
import com.rideshare.platform.pricing.dto.PricingFactorResponse;
import com.rideshare.platform.pricing.service.PricingFactorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** FR: Section 16 Admin Portal - Pricing Engine factors (pricing_factors table), one row per FareCalculator input. */
@RestController
@RequestMapping("/api/v1/admin/pricing-factors")
@RequiredArgsConstructor
@Tag(name = "Admin - Pricing")
public class AdminPricingController {

    private final PricingFactorService pricingFactorService;

    @GetMapping
    public ApiResponse<List<PricingFactorResponse>> list(@RequestParam(required = false) String calculator) {
        return ApiResponse.ok(pricingFactorService.list(calculator));
    }

    @PostMapping
    public ApiResponse<PricingFactorResponse> create(@Valid @RequestBody PricingFactorRequest request) {
        return ApiResponse.ok(pricingFactorService.create(request), "Pricing factor created.");
    }

    @PutMapping("/{id}")
    public ApiResponse<PricingFactorResponse> update(@PathVariable Long id, @Valid @RequestBody PricingFactorRequest request) {
        return ApiResponse.ok(pricingFactorService.update(id, request), "Pricing factor updated.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        pricingFactorService.delete(id);
        return ApiResponse.ok(null, "Pricing factor deleted.");
    }
}

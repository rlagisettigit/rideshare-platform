package com.rideshare.platform.coupon.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons")
public class CouponController {

    private final CouponService couponService;

    public record ApplyCouponRequest(String code, BigDecimal fare) {}

    @PostMapping("/apply")
    public ApiResponse<BigDecimal> apply(@RequestBody ApplyCouponRequest request) {
        return ApiResponse.ok(couponService.applyCoupon(request.code(), request.fare()));
    }
}

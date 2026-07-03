package com.rideshare.platform.coupon.service;

import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.coupon.entity.Coupon;
import com.rideshare.platform.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public BigDecimal applyCoupon(String code, BigDecimal fare) {
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> ApiException.badRequest("COUPON_001", "Invalid or inactive coupon code."));

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())
                || coupon.getValidTo() != null && now.isAfter(coupon.getValidTo())) {
            throw ApiException.badRequest("COUPON_002", "Coupon is not valid at this time.");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw ApiException.badRequest("COUPON_003", "Coupon usage limit reached.");
        }

        BigDecimal discount = "PERCENT".equals(coupon.getDiscountType())
                ? fare.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100))
                : coupon.getDiscountValue();
        if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
            discount = coupon.getMaxDiscount();
        }

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        return fare.subtract(discount).max(BigDecimal.ZERO);
    }
}

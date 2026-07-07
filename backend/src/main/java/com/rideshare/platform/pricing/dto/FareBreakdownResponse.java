package com.rideshare.platform.pricing.dto;

import com.rideshare.platform.pricing.service.FareBreakdown;

import java.math.BigDecimal;
import java.util.List;

public record FareBreakdownResponse(
        List<FareLineItem> lines,
        BigDecimal rideCostPerSeat,
        BigDecimal farePreDiscount,
        BigDecimal discountAmount,
        BigDecimal fareAfterDiscount,
        BigDecimal taxAmount,
        BigDecimal commissionAmount,
        BigDecimal passengerFare,
        BigDecimal driverPayout,
        String disclaimer
) {
    /** Shown next to the quoted price wherever a passenger sees an estimate before booking. */
    public static final String DISCLAIMER =
            "*Estimated fare - the actual price may vary based on demand, traffic, time of day, weather and other "
                    + "conditions at the time of booking.";

    public static FareBreakdownResponse from(FareBreakdown b) {
        List<FareLineItem> lines = b.lines().stream()
                .map(l -> new FareLineItem(l.calculator(), l.label(), l.amount()))
                .toList();
        return new FareBreakdownResponse(lines, b.rideCostPerSeat(), b.farePreDiscount(), b.discountAmount(),
                b.fareAfterDiscount(), b.taxAmount(), b.commissionAmount(), b.passengerFare(), b.driverPayout(),
                DISCLAIMER);
    }
}

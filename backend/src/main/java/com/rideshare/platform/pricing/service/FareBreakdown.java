package com.rideshare.platform.pricing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable accumulator threaded through the FareCalculator chain. Most calculators call
 * {@link #addLine} to record a labelled component and fold its amount into the running total
 * that becomes the passenger-facing fare; {@link #addInfoLine} records a component (e.g.
 * commission) that must NOT change what the passenger is charged.
 *
 * Ordering contract relied on by PricingEngine's calculator chain:
 *   Base..VehicleMultiplier build up {@code runningAmount} = per-seat ride cost.
 *   SeatPricing reads that as {@link #rideCostPerSeat} and turns it into the seats-adjusted
 *   {@link #farePreDiscount}, becoming the new {@code runningAmount}.
 *   Discount subtracts from runningAmount -> {@link #fareAfterDiscount}.
 *   Commission reads runningAmount (post-discount, pre-tax) into {@link #commissionAmount}
 *   without touching it. Tax adds to runningAmount -> final passenger fare.
 */
public class FareBreakdown {

    public record Line(String calculator, String label, BigDecimal amount) {}

    private final List<Line> lines = new ArrayList<>();
    private BigDecimal runningAmount = BigDecimal.ZERO;

    private BigDecimal rideCostPerSeat;
    private BigDecimal farePreDiscount;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal fareAfterDiscount;
    private BigDecimal commissionAmount = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal passengerFare;
    private BigDecimal driverPayout;

    public void addLine(String calculator, String label, BigDecimal amount) {
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        lines.add(new Line(calculator, label, scaled));
        runningAmount = runningAmount.add(scaled);
    }

    public void addInfoLine(String calculator, String label, BigDecimal amount) {
        lines.add(new Line(calculator, label, amount.setScale(2, RoundingMode.HALF_UP)));
    }

    public BigDecimal runningAmount() { return runningAmount; }
    public void setRunningAmount(BigDecimal amount) { this.runningAmount = money(amount); }

    public List<Line> lines() { return Collections.unmodifiableList(lines); }

    public BigDecimal rideCostPerSeat() { return rideCostPerSeat; }
    public void setRideCostPerSeat(BigDecimal v) { this.rideCostPerSeat = money(v); }

    public BigDecimal farePreDiscount() { return farePreDiscount; }
    public void setFarePreDiscount(BigDecimal v) { this.farePreDiscount = money(v); }

    public BigDecimal discountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal v) { this.discountAmount = money(v); }

    public BigDecimal fareAfterDiscount() { return fareAfterDiscount; }
    public void setFareAfterDiscount(BigDecimal v) { this.fareAfterDiscount = money(v); }

    public BigDecimal commissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal v) { this.commissionAmount = money(v); }

    public BigDecimal taxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal v) { this.taxAmount = money(v); }

    public BigDecimal passengerFare() { return passengerFare; }
    public void setPassengerFare(BigDecimal v) { this.passengerFare = money(v); }

    public BigDecimal driverPayout() { return driverPayout; }
    public void setDriverPayout(BigDecimal v) { this.driverPayout = money(v); }

    /** Every currency field on this breakdown is rounded to 2dp here, regardless of how many
     *  decimal places the calculator's own division produced (e.g. percent/100 doesn't round). */
    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}

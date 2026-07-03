package com.rideshare.platform.driver.dto;

import com.rideshare.platform.driver.entity.DriverStatus;

public record DriverResponse(
        Long id,
        String status,
        boolean online,
        String rejectionReason
) {
    public static DriverResponse from(Long id, DriverStatus status, boolean online, String rejectionReason) {
        return new DriverResponse(id, status.name(), online, rejectionReason);
    }
}

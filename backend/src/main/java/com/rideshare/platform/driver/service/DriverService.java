package com.rideshare.platform.driver.service;

import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.driver.dto.DriverOnboardRequest;
import com.rideshare.platform.driver.dto.DriverResponse;
import com.rideshare.platform.driver.entity.Driver;
import com.rideshare.platform.driver.entity.DriverStatus;
import com.rideshare.platform.driver.repository.DriverRepository;
import com.rideshare.platform.user.entity.User;
import com.rideshare.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** FR: Section 4 Driver Requirements. */
@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    @Transactional
    public DriverResponse onboard(String userPublicId, DriverOnboardRequest request) {
        User user = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> ApiException.notFound("USER_001", "User not found."));

        Driver driver = driverRepository.findByUserId(user.getId()).orElseGet(Driver::new);
        driver.setUser(user);
        driver.setLicenseNumber(request.licenseNumber());
        driver.setLicenseDocUrl(request.licenseDocUrl());
        driver.setGovernmentIdType(request.governmentIdType());
        driver.setGovernmentIdDocUrl(request.governmentIdDocUrl());
        driver.setAddressProofDocUrl(request.addressProofDocUrl());
        driver.setSelfieDocUrl(request.selfieDocUrl());
        driver.setStatus(DriverStatus.PENDING); // re-submission always resets to PENDING for re-verification

        user.setRoleDriver(true);
        userRepository.save(user);
        driverRepository.save(driver);

        return DriverResponse.from(driver.getId(), driver.getStatus(), driver.isOnline(), driver.getRejectionReason());
    }

    @Transactional
    public DriverResponse setAvailability(String userPublicId, boolean online) {
        Driver driver = getVerifiedDriver(userPublicId);
        driver.setOnline(online);
        driver.setLastOnlineAt(LocalDateTime.now());
        driverRepository.save(driver);
        return DriverResponse.from(driver.getId(), driver.getStatus(), driver.isOnline(), driver.getRejectionReason());
    }

    /** Used by RideService to enforce FR: "Online drivers can publish rides." */
    public Driver getVerifiedDriver(String userPublicId) {
        Driver driver = driverRepository.findByUserPublicId(userPublicId)
                .orElseThrow(() -> ApiException.notFound("DRIVER_001", "Driver profile not found."));
        if (driver.getStatus() != DriverStatus.VERIFIED) {
            throw ApiException.businessRule("DRIVER_002", "Driver is not verified yet.");
        }
        return driver;
    }

    /** Publishing a ride is itself a declaration of availability, so it implicitly goes online
     *  rather than requiring a separate manual toggle first - covers offering a ride that
     *  departs in just a few minutes. */
    @Transactional
    public void markOnline(Driver driver) {
        if (!driver.isOnline()) {
            driver.setOnline(true);
            driver.setLastOnlineAt(LocalDateTime.now());
            driverRepository.save(driver);
        }
    }

    @Transactional
    public DriverResponse review(Long driverId, boolean approve, String rejectionReason) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> ApiException.notFound("DRIVER_003", "Driver not found."));
        driver.setStatus(approve ? DriverStatus.VERIFIED : DriverStatus.REJECTED);
        driver.setRejectionReason(approve ? null : rejectionReason);
        driverRepository.save(driver);
        return DriverResponse.from(driver.getId(), driver.getStatus(), driver.isOnline(), driver.getRejectionReason());
    }
}

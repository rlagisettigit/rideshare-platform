package com.rideshare.platform.driver.repository;

import com.rideshare.platform.driver.entity.Driver;
import com.rideshare.platform.driver.entity.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByUserId(Long userId);
    Optional<Driver> findByUserPublicId(String publicId);
    long countByStatus(DriverStatus status);
}

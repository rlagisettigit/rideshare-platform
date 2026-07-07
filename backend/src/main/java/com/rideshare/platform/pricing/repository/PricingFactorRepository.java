package com.rideshare.platform.pricing.repository;

import com.rideshare.platform.pricing.entity.PricingFactor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PricingFactorRepository extends JpaRepository<PricingFactor, Long> {
    List<PricingFactor> findByCalculatorAndActiveTrue(String calculator);
    List<PricingFactor> findByCalculatorOrderByVehicleCategoryAscFactorKeyAsc(String calculator);
}

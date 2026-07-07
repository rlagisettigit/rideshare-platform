package com.rideshare.platform.pricing.service;

import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.pricing.dto.PricingFactorRequest;
import com.rideshare.platform.pricing.dto.PricingFactorResponse;
import com.rideshare.platform.pricing.entity.PricingFactor;
import com.rideshare.platform.pricing.repository.PricingFactorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Admin CRUD over pricing_factors - lets ops tune fare factors per calculator/vehicle category without a redeploy. */
@Service
@RequiredArgsConstructor
public class PricingFactorService {

    private final PricingFactorRepository pricingFactorRepository;

    @Transactional(readOnly = true)
    public List<PricingFactorResponse> list(String calculator) {
        List<PricingFactor> factors = calculator == null
                ? pricingFactorRepository.findAll()
                : pricingFactorRepository.findByCalculatorOrderByVehicleCategoryAscFactorKeyAsc(calculator);
        return factors.stream().map(PricingFactorResponse::from).toList();
    }

    @Transactional
    public PricingFactorResponse create(PricingFactorRequest request) {
        PricingFactor factor = new PricingFactor();
        apply(factor, request);
        pricingFactorRepository.save(factor);
        return PricingFactorResponse.from(factor);
    }

    @Transactional
    public PricingFactorResponse update(Long id, PricingFactorRequest request) {
        PricingFactor factor = pricingFactorRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("PRICING_001", "Pricing factor not found."));
        apply(factor, request);
        pricingFactorRepository.save(factor);
        return PricingFactorResponse.from(factor);
    }

    @Transactional
    public void delete(Long id) {
        if (!pricingFactorRepository.existsById(id)) {
            throw ApiException.notFound("PRICING_001", "Pricing factor not found.");
        }
        pricingFactorRepository.deleteById(id);
    }

    private void apply(PricingFactor factor, PricingFactorRequest request) {
        factor.setCalculator(request.calculator());
        factor.setVehicleCategory(request.vehicleCategory());
        factor.setFactorKey(request.factorKey());
        factor.setFactorValue(request.factorValue());
        factor.setValueType(request.valueType() != null ? request.valueType() : "FLAT");
        factor.setRangeStart(request.rangeStart());
        factor.setRangeEnd(request.rangeEnd());
        factor.setActive(request.active());
        factor.setDescription(request.description());
    }
}

package com.rideshare.platform.payment.service;

import com.rideshare.platform.payment.entity.Payment;
import com.rideshare.platform.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * FR: External Systems - Payment Gateway integration point.
 * Idempotency-key protected so retried client requests never double-charge (Section 22).
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment initiate(Long bookingId, BigDecimal amount, String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {
                    Payment payment = new Payment();
                    payment.setBookingId(bookingId);
                    payment.setAmount(amount);
                    payment.setIdempotencyKey(idempotencyKey);
                    payment.setProvider("GATEWAY_ABSTRACTION"); // TODO: wire real Payment Gateway SDK
                    payment.setStatus("INITIATED");
                    return paymentRepository.save(payment);
                });
    }

    /**
     * Captures a fare on ride completion. There's no real gateway wired in yet (see initiate()),
     * so this simulates an immediate successful capture rather than the usual async
     * initiate-then-webhook-callback flow a real gateway would use.
     */
    @Transactional
    public Payment capture(Long bookingId, BigDecimal amount, String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {
                    Payment payment = new Payment();
                    payment.setBookingId(bookingId);
                    payment.setAmount(amount);
                    payment.setIdempotencyKey(idempotencyKey);
                    payment.setProvider("GATEWAY_ABSTRACTION"); // TODO: wire real Payment Gateway SDK
                    payment.setProviderRef("SIMULATED-" + UUID.randomUUID());
                    payment.setStatus("SUCCESS");
                    return paymentRepository.save(payment);
                });
    }
}

package com.rideshare.platform.payment.controller;

import com.rideshare.platform.common.ApiResponse;
import com.rideshare.platform.payment.entity.Payment;
import com.rideshare.platform.payment.repository.PaymentRepository;
import com.rideshare.platform.payment.service.PaymentService;
import com.rideshare.platform.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public record InitiatePaymentRequest(Long bookingId, BigDecimal amount) {}

    @PostMapping
    public ApiResponse<Payment> initiate(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                          @RequestBody InitiatePaymentRequest request) {
        return ApiResponse.ok(paymentService.initiate(request.bookingId(), request.amount(), idempotencyKey));
    }

    @GetMapping("/me")
    public ApiResponse<List<Payment>> myPayments(@AuthenticationPrincipal String userPublicId) {
        Long userId = userRepository.findByPublicId(userPublicId).orElseThrow().getId();
        return ApiResponse.ok(paymentRepository.findByPassengerId(userId));
    }
}

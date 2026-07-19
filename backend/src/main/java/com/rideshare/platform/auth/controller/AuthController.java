package com.rideshare.platform.auth.controller;

import com.rideshare.platform.auth.dto.*;
import com.rideshare.platform.auth.service.AuthService;
import com.rideshare.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** FR-001 User Registration, FR-002 Login. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "Account created.");
    }

    @PostMapping("/otp/request")
    public ApiResponse<Void> requestOtp(@Valid @RequestBody OtpRequest request) {
        authService.requestOtp(request);
        return ApiResponse.ok(null, "OTP sent.");
    }

    @PostMapping("/mobile/verify")
    public ApiResponse<Void> verifyMobile(@Valid @RequestBody VerifyMobileRequest request) {
        authService.verifyMobile(request);
        return ApiResponse.ok(null, "Mobile number verified.");
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }
}

package com.rideshare.platform.auth.service;

import com.rideshare.platform.auth.dto.*;
import com.rideshare.platform.auth.entity.RefreshToken;
import com.rideshare.platform.auth.oidc.OidcIdTokenVerifier;
import com.rideshare.platform.auth.oidc.OidcProviderProperties;
import com.rideshare.platform.auth.oidc.OidcVerificationConfig;
import com.rideshare.platform.auth.oidc.VerifiedIdToken;
import com.rideshare.platform.auth.repository.RefreshTokenRepository;
import com.rideshare.platform.common.AgeValidator;
import com.rideshare.platform.common.exception.ApiException;
import com.rideshare.platform.driver.entity.Driver;
import com.rideshare.platform.driver.entity.DriverStatus;
import com.rideshare.platform.driver.repository.DriverRepository;
import com.rideshare.platform.security.JwtService;
import com.rideshare.platform.user.entity.IdentityProvider;
import com.rideshare.platform.user.entity.User;
import com.rideshare.platform.user.entity.UserStatus;
import com.rideshare.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** FR-001 Registration, FR-002 Login (Mobile+OTP, Email+Password, Google, Apple). */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final OidcIdTokenVerifier oidcIdTokenVerifier;
    private final OidcProviderProperties oidcProviderProperties;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.conflict("AUTH_001", "Email is already registered.");
        }
        if (userRepository.existsByMobile(request.mobile())) {
            throw ApiException.conflict("AUTH_002", "Mobile number is already registered.");
        }
        AgeValidator.requireAtLeast18(request.dob());

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setMobile(request.mobile());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setGender(request.gender());
        user.setDob(request.dob());
        user.setRolePassenger(true);
        user.setRoleDriver(request.asDriver());
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        if (request.asDriver()) {
            // TEMPORARY: KYC onboarding/admin review UI isn't built yet, so auto-verify
            // drivers at signup to unblock vehicle registration and going online.
            // Remove once the onboarding flow ships and re-require DriverService.onboard + review.
            Driver driver = new Driver();
            driver.setUser(user);
            driver.setLicenseNumber("PENDING-" + user.getPublicId());
            driver.setStatus(DriverStatus.VERIFIED);
            driverRepository.save(driver);
        }

        return issueTokens(user);
    }

    public void requestOtp(OtpRequest request) {
        otpService.issueOtp(request.mobile(), request.purpose() == null ? "LOGIN" : request.purpose());
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = switch (request.mode()) {
            case "MOBILE_OTP" -> loginWithOtp(request);
            case "GOOGLE" -> loginWithSocialProvider(request, "GOOGLE");
            case "APPLE" -> loginWithSocialProvider(request, "APPLE");
            default -> loginWithEmailPassword(request);
        };
        return issueTokens(user);
    }

    private User loginWithEmailPassword(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> ApiException.unauthorized("AUTH_003", "Invalid email or password."));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("AUTH_003", "Invalid email or password.");
        }
        return user;
    }

    private User loginWithOtp(LoginRequest request) {
        otpService.verifyOtp(request.mobile(), "LOGIN", request.otp());
        return userRepository.findByMobile(request.mobile())
                .orElseThrow(() -> ApiException.notFound("AUTH_004", "No account registered with this mobile."));
    }

    private User loginWithSocialProvider(LoginRequest request, String provider) {
        OidcVerificationConfig config = oidcProviderProperties.forProvider(provider)
                .orElseThrow(() -> ApiException.externalService("AUTH_005", provider + " sign-in is not configured in this environment."));

        VerifiedIdToken verified = oidcIdTokenVerifier.verify(request.idToken(), config);
        if (verified.email() == null || !verified.emailVerified()) {
            throw ApiException.unauthorized("AUTH_008", "This " + provider + " account has no verified email.");
        }

        // Matched by verified email rather than provider+subject: a user who originally
        // registered with email/password and later taps "Continue with Google" using the same,
        // provider-verified address logs into their existing account rather than getting a
        // second, disconnected one. The provider has already cryptographically proven they
        // control that address, so this is standard account-linking-by-verified-email behavior.
        return userRepository.findByEmail(verified.email())
                .orElseGet(() -> provisionSocialUser(verified, provider));
    }

    private User provisionSocialUser(VerifiedIdToken verified, String provider) {
        User user = new User();
        user.setName(verified.name() != null && !verified.name().isBlank() ? verified.name() : verified.email());
        user.setEmail(verified.email());
        user.setMobile(null); // not present on an ID token; see V13__nullable_mobile_for_social_signup.sql
        user.setRolePassenger(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setIdentityProvider("GOOGLE".equals(provider) ? IdentityProvider.GOOGLE : IdentityProvider.APPLE);
        return userRepository.save(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        String hash = passwordEncoder.encode(request.refreshToken()); // illustrative; real impl compares raw hash lookup
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(request.refreshToken())
                .orElseThrow(() -> ApiException.unauthorized("AUTH_006", "Invalid refresh token."));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.unauthorized("AUTH_007", "Refresh token expired.");
        }
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> ApiException.notFound("USER_001", "User not found."));

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(user);
    }

    private TokenResponse issueTokens(User user) {
        List<String> roles = new ArrayList<>();
        if (user.isRolePassenger()) roles.add("PASSENGER");
        if (user.isRoleDriver()) roles.add("DRIVER");
        if (user.isRoleAdmin()) roles.add("ADMIN");

        String access = jwtService.generateAccessToken(user.getPublicId(), roles);
        String refresh = jwtService.generateRefreshToken(user.getPublicId());

        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setTokenHash(refresh); // storing raw token here for lookup simplicity; hash in production
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(rt);

        return TokenResponse.of(access, refresh);
    }
}

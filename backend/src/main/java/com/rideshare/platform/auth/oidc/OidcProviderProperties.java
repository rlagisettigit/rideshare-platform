package com.rideshare.platform.auth.oidc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Which client IDs (audiences) this backend accepts Google/Apple ID tokens for. Comma-separated
 * so a web client ID can be added now and mobile (iOS/Android) client IDs added later without a
 * code change - see the app-wide mobile plan. Issuer and JWKS URL are fixed per provider (not
 * environment-configurable, they never change) and live here alongside the audience config.
 *
 * If a provider's env var is unset, {@link #forProvider} returns empty and AuthService reports
 * "sign-in is not configured" - the same behavior as before this was implemented, until an
 * operator actually registers OAuth client IDs with Google/Apple.
 */
@Component
public class OidcProviderProperties {

    // Google issues tokens with either form as `iss` depending on flow; both are valid.
    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    private static final String GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";

    private static final Set<String> APPLE_ISSUERS = Set.of("https://appleid.apple.com");
    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";

    private final Set<String> googleClientIds;
    private final Set<String> appleClientIds;

    public OidcProviderProperties(
            @Value("${GOOGLE_CLIENT_IDS:}") String googleClientIds,
            @Value("${APPLE_CLIENT_IDS:}") String appleClientIds) {
        this.googleClientIds = parse(googleClientIds);
        this.appleClientIds = parse(appleClientIds);
    }

    public Optional<OidcVerificationConfig> forProvider(String provider) {
        return switch (provider) {
            case "GOOGLE" -> googleClientIds.isEmpty() ? Optional.empty()
                    : Optional.of(new OidcVerificationConfig(GOOGLE_ISSUERS, GOOGLE_JWKS_URL, googleClientIds));
            case "APPLE" -> appleClientIds.isEmpty() ? Optional.empty()
                    : Optional.of(new OidcVerificationConfig(APPLE_ISSUERS, APPLE_JWKS_URL, appleClientIds));
            default -> Optional.empty();
        };
    }

    private Set<String> parse(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}

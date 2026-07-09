package com.rideshare.platform.auth.oidc;

import com.rideshare.platform.common.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.ProtectedHeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verifies a Google/Apple "Sign in with..." ID token: signature (against the provider's published
 * JWKS, keyed by `kid`), issuer, audience and expiry. Both providers publish standard RFC 7517 JWK
 * Sets, so one implementation covers both - only the issuer/JWKS URL/allowed audiences differ
 * (see OidcProviderProperties).
 */
@Component
@Slf4j
public class OidcIdTokenVerifier {

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final RestClient restClient = RestClient.create();
    private final Map<String, CachedJwkSet> jwksCache = new ConcurrentHashMap<>();

    private record CachedJwkSet(Map<String, RSAPublicKey> keysByKid, Instant fetchedAt) {}
    private record Jwk(String kid, String kty, String n, String e) {}
    private record JwkSet(List<Jwk> keys) {}

    public VerifiedIdToken verify(String idToken, OidcVerificationConfig config) {
        if (idToken == null || idToken.isBlank()) {
            throw ApiException.unauthorized("AUTH_008", "Missing sign-in token.");
        }

        Locator<Key> keyLocator = header -> header instanceof ProtectedHeader ph
                ? resolveKey(config.jwksUrl(), ph.getKeyId())
                : null;

        Claims claims;
        try {
            claims = Jwts.parser()
                    .keyLocator(keyLocator)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("ID token verification failed: {}", e.getMessage());
            throw ApiException.unauthorized("AUTH_008", "Invalid or expired sign-in token.");
        }

        if (!config.allowedIssuers().contains(claims.getIssuer())) {
            throw ApiException.unauthorized("AUTH_008", "Invalid or expired sign-in token.");
        }
        if (claims.getAudience().stream().noneMatch(config.allowedAudiences()::contains)) {
            throw ApiException.unauthorized("AUTH_008", "Invalid or expired sign-in token.");
        }

        String email = claims.get("email", String.class);
        Object emailVerifiedClaim = claims.get("email_verified", Object.class);
        boolean emailVerified = Boolean.TRUE.equals(emailVerifiedClaim) || "true".equals(String.valueOf(emailVerifiedClaim));
        String name = claims.get("name", String.class);

        return new VerifiedIdToken(claims.getSubject(), email, emailVerified, name);
    }

    /** Refetches the JWKS whenever the requested kid isn't cached yet - handles the provider rotating signing keys. */
    private RSAPublicKey resolveKey(String jwksUrl, String kid) {
        if (kid == null) return null;
        CachedJwkSet cached = jwksCache.get(jwksUrl);
        boolean stale = cached == null || Duration.between(cached.fetchedAt(), Instant.now()).compareTo(CACHE_TTL) > 0;
        if (stale || !cached.keysByKid().containsKey(kid)) {
            cached = fetchAndCache(jwksUrl);
        }
        return cached.keysByKid().get(kid);
    }

    private CachedJwkSet fetchAndCache(String jwksUrl) {
        Map<String, RSAPublicKey> byKid = new HashMap<>();
        try {
            JwkSet jwkSet = restClient.get().uri(jwksUrl).retrieve().body(JwkSet.class);
            if (jwkSet != null && jwkSet.keys() != null) {
                for (Jwk jwk : jwkSet.keys()) {
                    if ("RSA".equals(jwk.kty()) && jwk.kid() != null && jwk.n() != null && jwk.e() != null) {
                        byKid.put(jwk.kid(), toRsaPublicKey(jwk));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch JWKS from {}: {}", jwksUrl, e.getMessage());
        }
        CachedJwkSet result = new CachedJwkSet(byKid, Instant.now());
        jwksCache.put(jwksUrl, result);
        return result;
    }

    private RSAPublicKey toRsaPublicKey(Jwk jwk) {
        byte[] modulusBytes = Base64.getUrlDecoder().decode(jwk.n());
        byte[] exponentBytes = Base64.getUrlDecoder().decode(jwk.e());
        BigInteger modulus = new BigInteger(1, modulusBytes);
        BigInteger exponent = new BigInteger(1, exponentBytes);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct RSA public key from JWK kid=" + jwk.kid(), e);
        }
    }
}

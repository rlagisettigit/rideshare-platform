package com.rideshare.platform.auth.oidc;

/** Claims extracted from a Google/Apple ID token whose signature, issuer and audience have already been verified. */
public record VerifiedIdToken(String subject, String email, boolean emailVerified, String name) {}

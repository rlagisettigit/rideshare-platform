package com.rideshare.platform.auth.oidc;

import java.util.Set;

/** Per-provider parameters an ID token must satisfy: who could have issued it, where to fetch signing keys, who it must be issued for. */
public record OidcVerificationConfig(Set<String> allowedIssuers, String jwksUrl, Set<String> allowedAudiences) {}

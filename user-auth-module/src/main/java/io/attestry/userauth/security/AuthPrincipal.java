package io.attestry.userauth.security;

import io.attestry.userauth.domain.auth.model.VerificationLevel;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
// TODO("추후 제거")
public record AuthPrincipal(
    String tokenId,
    String userId,
    String tenantId,
    VerificationLevel verificationLevel,
    Set<String> scopes,
    Instant expiresAt
) {
    public static AuthPrincipal issue(
        String userId,
        String tenantId,
        VerificationLevel verificationLevel,
        Set<String> scopes,
        Instant now,
        Duration ttl
    ) {
        return new AuthPrincipal(
            UUID.randomUUID().toString(),
            userId,
            tenantId,
            verificationLevel,
            scopes,
            now.plus(ttl)
        );
    }
}

package io.attestry.userauth.domain.auth.model;

import io.attestry.userauth.domain.user.enums.VerificationLevel;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AuthPrincipal(
    String tokenId,
    String userId,
    String tenantId,
    String groupId,
    VerificationLevel verificationLevel,
    Set<String> scopes,
    Instant expiresAt
) {
    public static AuthPrincipal issue(
        String userId,
        String tenantId,
        String groupId,
        VerificationLevel verificationLevel,
        Set<String> scopes,
        Instant now,
        Duration ttl
    ) {
        return new AuthPrincipal(
            UUID.randomUUID().toString(),
            userId,
            tenantId,
            groupId,
            verificationLevel,
            scopes,
            now.plus(ttl)
        );
    }
}

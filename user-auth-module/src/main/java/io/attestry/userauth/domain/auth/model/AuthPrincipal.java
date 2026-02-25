package io.attestry.userauth.domain.auth.model;

import io.attestry.userauth.domain.user.enums.VerificationLevel;
import java.time.Instant;
import java.util.Set;

public record AuthPrincipal(
    String tokenId,
    String userId,
    String tenantId,
    String groupId,
    VerificationLevel verificationLevel,
    Set<Scope> scopes,
    Instant expiresAt
) {
}

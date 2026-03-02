package io.attestry.userauth.application.dto.command;

import io.attestry.userauth.domain.user.enums.VerificationLevel;
import java.time.Instant;
import java.util.Set;

public record ActorContext(
    String tokenId,
    String userId,
    String tenantId,
    String groupId,
    VerificationLevel verificationLevel,
    Set<String> scopes,
    Instant expiresAt
) {
}

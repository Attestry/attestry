package io.attestry.userauth.application.dto.command;

import io.attestry.userauth.domain.identity.model.VerificationLevel;
import io.attestry.userauth.security.AuthPrincipal;
import java.time.Instant;
import java.util.Set;

public record ActorContext(
    String tokenId,
    String userId,
    String tenantId,
    VerificationLevel verificationLevel,
    Set<String> scopes,
    Instant expiresAt
) {
    public static ActorContext from(AuthPrincipal principal) {
        return new ActorContext(
            principal.tokenId(),
            principal.userId(),
            principal.tenantId(),
            principal.verificationLevel(),
            principal.scopes(),
            principal.expiresAt()
        );
    }
}

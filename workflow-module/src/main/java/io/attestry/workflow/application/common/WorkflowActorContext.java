package io.attestry.workflow.application.common;

import io.attestry.userauth.domain.auth.model.VerificationLevel;
import io.attestry.userauth.security.AuthPrincipal;
import java.time.Instant;
import java.util.Set;

public record WorkflowActorContext(
    String tokenId,
    String userId,
    String tenantId,
    VerificationLevel verificationLevel,
    Set<String> scopes,
    Instant expiresAt
) {
    public static WorkflowActorContext from(AuthPrincipal principal) {
        return new WorkflowActorContext(
            principal.tokenId(),
            principal.userId(),
            principal.tenantId(),
            principal.verificationLevel(),
            principal.scopes(),
            principal.expiresAt()
        );
    }
}

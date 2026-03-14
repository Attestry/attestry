package io.attestry.userauth.contract.authorization;

import java.util.Set;

public interface AuthorizationCheckPort {

    AuthorizationDecision authorize(AuthorizationCheckCommand command);

    record AuthorizationCheckCommand(
        String userId,
        String actorTenantId,
        String targetTenantId,
        Set<String> tokenScopes,
        String action,
        String resourceRef,
        DecisionMode decisionMode
    ) {
    }

    record AuthorizationDecision(boolean allowed, String reasonCode) {
    }

    enum DecisionMode {
        TOKEN_SNAPSHOT,
        LIVE_RECHECK
    }
}

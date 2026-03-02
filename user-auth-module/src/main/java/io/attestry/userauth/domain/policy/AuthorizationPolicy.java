package io.attestry.userauth.domain.policy;

import io.attestry.userauth.application.dto.command.ActorContext;

public final class AuthorizationPolicy {

    private AuthorizationPolicy() {
    }

    public static boolean isAllowed(ActorContext actor, String resourceTenantId, String requiredScope) {
        return TenantIsolationPolicy.isIsolated(actor.tenantId(), resourceTenantId)
            && actor.scopes().contains(requiredScope);
    }

}

package io.attestry.userauth.domain.authorization.policy;

import java.util.Set;

public final class AuthorizationPolicy {

    private AuthorizationPolicy() {
    }

    public static boolean isAllowed(
        String actorTenantId,
        Set<String> actorScopes,
        String resourceTenantId,
        String requiredScope
    ) {
        return TenantIsolationPolicy.isIsolated(actorTenantId, resourceTenantId)
            && actorScopes != null
            && actorScopes.contains(requiredScope);
    }

}

package io.attestry.userauth.domain.policy;

import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.auth.model.Scope;

public final class AuthorizationPolicy {

    private AuthorizationPolicy() {
    }

    public static boolean isAllowed(AuthPrincipal principal, String resourceTenantId, Scope requiredScope) {
        return TenantIsolationPolicy.isIsolated(principal.tenantId(), resourceTenantId)
            && principal.scopes().contains(requiredScope);
    }
}

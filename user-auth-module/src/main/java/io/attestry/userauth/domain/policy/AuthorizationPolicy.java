package io.attestry.userauth.domain.policy;

import io.attestry.userauth.domain.auth.model.AuthPrincipal;

public final class AuthorizationPolicy {

    private AuthorizationPolicy() {
    }

    public static boolean isAllowed(AuthPrincipal principal, String resourceTenantId, String requiredScope) {
        return TenantIsolationPolicy.isIsolated(principal.tenantId(), resourceTenantId)
            && principal.scopes().contains(requiredScope);
    }
}

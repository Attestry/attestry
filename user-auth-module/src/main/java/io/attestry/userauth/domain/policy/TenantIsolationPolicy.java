package io.attestry.userauth.domain.policy;

public final class TenantIsolationPolicy {

    private TenantIsolationPolicy() {
    }

    public static boolean isIsolated(String tokenTenantId, String resourceTenantId) {
        if (tokenTenantId == null || resourceTenantId == null) {
            return false;
        }
        return tokenTenantId.equals(resourceTenantId);
    }
}

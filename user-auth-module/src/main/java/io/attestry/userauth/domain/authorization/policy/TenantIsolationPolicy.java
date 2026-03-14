package io.attestry.userauth.domain.authorization.policy;

public final class TenantIsolationPolicy {

    private TenantIsolationPolicy() {
    }

    public static boolean isIsolated(String tokenTenantId, String resourceTenantId) {
        if (resourceTenantId == null) {
            return true;
        }
        if (tokenTenantId == null) {
            return false;
        }
        return tokenTenantId.equals(resourceTenantId);
    }
}

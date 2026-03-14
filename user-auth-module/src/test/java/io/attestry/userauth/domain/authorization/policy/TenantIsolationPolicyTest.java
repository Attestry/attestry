package io.attestry.userauth.domain.authorization.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TenantIsolationPolicyTest {

    @Test
    void shouldAllowWhenTenantIdsMatch() {
        assertTrue(TenantIsolationPolicy.isIsolated("tenant-a", "tenant-a"));
    }

    @Test
    void shouldDenyWhenTenantIdsDiffer() {
        assertFalse(TenantIsolationPolicy.isIsolated("tenant-a", "tenant-b"));
    }
}

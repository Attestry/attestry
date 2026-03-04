package io.attestry.userauth.domain.authorization.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthorizationPolicyTest {

    @Test
    void shouldAllowWhenScopeAndTenantMatch() {
        assertTrue(
            AuthorizationPolicy.isAllowed(
                "tenant-a",
                Set.of(PermissionCodes.BRAND_MINT),
                "tenant-a",
                PermissionCodes.BRAND_MINT
            )
        );
    }

    @Test
    void shouldDenyWhenScopeMissing() {
        assertFalse(
            AuthorizationPolicy.isAllowed(
                "tenant-a",
                Set.of(PermissionCodes.OWNER_TRANSFER_CREATE),
                "tenant-a",
                PermissionCodes.BRAND_MINT
            )
        );
    }

    @Test
    void shouldDenyWhenTenantDifferent() {
        assertFalse(
            AuthorizationPolicy.isAllowed(
                "tenant-a",
                Set.of(PermissionCodes.BRAND_MINT),
                "tenant-b",
                PermissionCodes.BRAND_MINT
            )
        );
    }
}

package io.attestry.userauth.domain.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.domain.auth.model.RoleCodes;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoleAssignmentPolicyTest {

    private final RoleAssignmentPolicy policy = new DefaultRoleAssignmentPolicy();

    @Test
    void platformAdminCanAssignSensitiveRoles() {
        assertTrue(policy.canAssign(
            Set.of(RoleCodes.PLATFORM_SUPER_ADMIN),
            RoleCodes.TENANT_OWNER
        ));
    }

    @Test
    void tenantOwnerCanAssignTenantOperator() {
        assertTrue(policy.canAssign(
            Set.of(RoleCodes.TENANT_OWNER),
            RoleCodes.TENANT_OPERATOR
        ));
    }

    @Test
    void tenantOwnerCanAssignTenantOwnerToOthers() {
        assertTrue(policy.canAssign(
            Set.of(RoleCodes.TENANT_OWNER),
            RoleCodes.TENANT_OWNER
        ));
    }

    @Test
    void deprecatedRoleCannotBeAssignedEvenByPlatformAdmin() {
        assertFalse(policy.canAssign(
            Set.of(RoleCodes.PLATFORM_SUPER_ADMIN),
            "BRAND_OPERATOR"
        ));
    }

    @Test
    void sensitiveRoleRequiresLiveRecheck() {
        assertTrue(policy.requiresLiveRecheck(RoleCodes.TENANT_OWNER));
        assertFalse(policy.requiresLiveRecheck(RoleCodes.TENANT_OPERATOR));
    }

    @Test
    void selfEscalationDeniedForSensitiveRoles() {
        assertTrue(policy.isSelfEscalationDenied(
            "membership-1",
            "membership-1",
            RoleCodes.TENANT_OWNER
        ));
        assertFalse(policy.isSelfEscalationDenied(
            "membership-1",
            "membership-1",
            RoleCodes.TENANT_OPERATOR
        ));
    }
}

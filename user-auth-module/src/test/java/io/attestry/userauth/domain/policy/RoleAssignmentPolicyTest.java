package io.attestry.userauth.domain.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.domain.auth.model.RoleCodes;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoleAssignmentPolicyTest {

    @Test
    void platformAdminCanAssignSensitiveRoles() {
        assertTrue(RoleAssignmentPolicy.canAssign(
            Set.of(RoleCodes.PLATFORM_SUPER_ADMIN),
            RoleCodes.TENANT_OWNER
        ));
    }

    @Test
    void tenantOwnerCanAssignTenantOperator() {
        assertTrue(RoleAssignmentPolicy.canAssign(
            Set.of(RoleCodes.TENANT_OWNER),
            RoleCodes.TENANT_OPERATOR
        ));
    }

    @Test
    void deprecatedRoleCannotBeAssignedEvenByPlatformAdmin() {
        assertFalse(RoleAssignmentPolicy.canAssign(
            Set.of(RoleCodes.PLATFORM_SUPER_ADMIN),
            "BRAND_OPERATOR"
        ));
    }

    @Test
    void sensitiveRoleRequiresLiveRecheck() {
        assertTrue(RoleAssignmentPolicy.requiresLiveRecheck(RoleCodes.TENANT_OWNER));
        assertFalse(RoleAssignmentPolicy.requiresLiveRecheck(RoleCodes.TENANT_OPERATOR));
    }

    @Test
    void selfEscalationDeniedForSensitiveRoles() {
        assertTrue(RoleAssignmentPolicy.isSelfEscalationDenied(
            "membership-1",
            "membership-1",
            RoleCodes.TENANT_OWNER
        ));
        assertFalse(RoleAssignmentPolicy.isSelfEscalationDenied(
            "membership-1",
            "membership-1",
            RoleCodes.TENANT_OPERATOR
        ));
    }
}

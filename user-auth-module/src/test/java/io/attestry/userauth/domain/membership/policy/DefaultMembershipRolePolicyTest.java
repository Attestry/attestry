package io.attestry.userauth.domain.membership.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.tenant.model.TenantType;
import org.junit.jupiter.api.Test;

class DefaultMembershipRolePolicyTest {

    @Test
    void shouldMapAdminToTenantOwner() {
        String roleCode = DefaultMembershipRolePolicy.resolveGlobalRoleCode(MembershipRole.ADMIN, TenantType.BRAND);
        assertEquals(RoleCodes.TENANT_OWNER, roleCode);
    }

    @Test
    void shouldMapOperatorToTenantOperator() {
        String roleCode = DefaultMembershipRolePolicy.resolveGlobalRoleCode(MembershipRole.OPERATOR, TenantType.RETAIL);
        assertEquals(RoleCodes.TENANT_OPERATOR, roleCode);
    }

    @Test
    void shouldMapStaffToTenantStaff() {
        String roleCode = DefaultMembershipRolePolicy.resolveGlobalRoleCode(MembershipRole.STAFF, TenantType.SERVICE);
        assertEquals(RoleCodes.TENANT_STAFF, roleCode);
    }

    @Test
    void shouldFallbackToTenantStaffWhenRoleIsNull() {
        String roleCode = DefaultMembershipRolePolicy.resolveGlobalRoleCode(null, TenantType.INTERNAL);
        assertEquals(RoleCodes.TENANT_STAFF, roleCode);
    }
}

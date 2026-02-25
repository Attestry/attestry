package io.attestry.userauth.domain.policy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.auth.model.Scope;
import io.attestry.userauth.domain.organization.model.TenantStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScopePolicyTest {

    @Test
    void shouldGrantRetailTransferScopesToRetailOperator() {
        Membership membership = new Membership(
            "m1",
            "u1",
            "g1",
            "t1",
            GroupType.RETAIL,
            MembershipRole.OPERATOR,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        );

        Set<Scope> scopes = ScopePolicy.forMembership(membership);

        assertTrue(scopes.contains(Scope.RETAIL_RELEASE));
        assertTrue(scopes.contains(Scope.RETAIL_TRANSFER_CREATE));
    }

    @Test
    void ownerDefaultScopesShouldAlwaysContainTransferPermissions() {
        Set<Scope> scopes = ScopePolicy.ownerDefaultScopes();

        assertTrue(scopes.contains(Scope.OWNER_TRANSFER_CREATE));
        assertTrue(scopes.contains(Scope.OWNER_TRANSFER_ACCEPT));
    }

    @Test
    void brandAdminShouldGetTenantAndBrandScopes() {
        Membership membership = new Membership(
            "m2",
            "u1",
            "g1",
            "t1",
            GroupType.BRAND,
            MembershipRole.ADMIN,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        );

        Set<Scope> scopes = ScopePolicy.forMembership(membership);

        assertTrue(scopes.contains(Scope.TENANT_ADMIN));
        assertTrue(scopes.contains(Scope.MEMBERSHIP_MANAGE));
        assertTrue(scopes.contains(Scope.BRAND_MINT));
        assertTrue(scopes.contains(Scope.BRAND_VOID));
    }

    @Test
    void retailStaffShouldGetNoPrivilegedScopes() {
        Membership membership = new Membership(
            "m3",
            "u2",
            "g2",
            "t1",
            GroupType.RETAIL,
            MembershipRole.STAFF,
            MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE,
            TenantStatus.ACTIVE
        );

        Set<Scope> scopes = ScopePolicy.forMembership(membership);

        assertTrue(scopes.isEmpty());
    }
}

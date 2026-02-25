package io.attestry.userauth.domain.policy;

import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.auth.model.Scope;
import java.util.EnumSet;
import java.util.Set;

public final class ScopePolicy {

    private ScopePolicy() {
    }

    public static Set<Scope> ownerDefaultScopes() {
        return EnumSet.of(
            Scope.OWNER_TRANSFER_CREATE,
            Scope.OWNER_TRANSFER_ACCEPT,
            Scope.OWNER_RISK_FLAG,
            Scope.OWNER_RISK_CLEAR
        );
    }

    public static Set<Scope> forMembership(Membership membership) {
        EnumSet<Scope> scopes = EnumSet.noneOf(Scope.class);
        MembershipRole role = membership.role();
        GroupType groupType = membership.groupType();

        if (role == MembershipRole.ADMIN) {
            scopes.add(Scope.TENANT_ADMIN);
            scopes.add(Scope.MEMBERSHIP_MANAGE);
            scopes.add(Scope.PASSPORT_PERMISSION_GRANT);
            scopes.add(Scope.TENANT_AUDIT_READ);
        }

        if (groupType == GroupType.BRAND) {
            if (role == MembershipRole.ADMIN || role == MembershipRole.OPERATOR) {
                scopes.add(Scope.BRAND_MINT);
                scopes.add(Scope.BRAND_VOID);
            }
        }

        if (groupType == GroupType.RETAIL) {
            if (role == MembershipRole.ADMIN || role == MembershipRole.OPERATOR) {
                scopes.add(Scope.RETAIL_RELEASE);
                scopes.add(Scope.RETAIL_TRANSFER_CREATE);
            }
        }

        return scopes;
    }
}

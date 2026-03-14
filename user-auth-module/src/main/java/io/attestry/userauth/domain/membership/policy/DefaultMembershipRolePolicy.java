package io.attestry.userauth.domain.membership.policy;

import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.tenant.model.TenantType;

public final class DefaultMembershipRolePolicy {

    private DefaultMembershipRolePolicy() {
    }

    public static String resolveGlobalRoleCode(MembershipRole membershipRole, TenantType groupType) {
        if (membershipRole == null) {
            return RoleCodes.TENANT_STAFF;
        }
        return switch (membershipRole) {
            case ADMIN -> RoleCodes.TENANT_OWNER;
            case OPERATOR -> RoleCodes.TENANT_OPERATOR;
            case STAFF -> RoleCodes.TENANT_STAFF;
        };
    }
}

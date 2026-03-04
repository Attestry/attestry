package io.attestry.userauth.domain.membership.policy;

import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.organization.model.GroupType;

public final class DefaultMembershipRolePolicy {

    private DefaultMembershipRolePolicy() {
    }

    public static String resolveGlobalRoleCode(MembershipRole membershipRole, GroupType groupType) {
        return RoleCodes.TENANT_STAFF;
    }
}

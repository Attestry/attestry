package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.membership.model.MembershipRole;

final class DefaultRoleIdMapper {

    private DefaultRoleIdMapper() {
    }

    static String map(MembershipRole role, GroupType groupType) {
        if (role == MembershipRole.ADMIN && groupType == GroupType.BRAND) {
            return "role-brand-admin-base";
        }
        if (role == MembershipRole.ADMIN && groupType == GroupType.RETAIL) {
            return "role-retail-admin-base";
        }
        if (role == MembershipRole.OPERATOR && groupType == GroupType.BRAND) {
            return "role-brand-operator";
        }
        if (role == MembershipRole.OPERATOR && groupType == GroupType.RETAIL) {
            return "role-retail-operator";
        }
        return "role-group-staff";
    }
}

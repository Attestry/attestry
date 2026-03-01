package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.membership.model.MembershipRole;

final class DefaultRoleIdMapper {

    private DefaultRoleIdMapper() {
    }

    static String map(MembershipRole role, GroupType groupType) {
        // v1: membership.role(직책)와 RBAC role(권한 번들) 분리.
        // 자동 매핑은 최소 base role만 부여하고, admin/sensitive role은 Assignment API로만 부여한다.
        if (groupType == GroupType.BRAND) {
            return "role-brand-operator";
        }
        if (groupType == GroupType.RETAIL) {
            return "role-retail-operator";
        }
        return "role-group-staff";
    }
}

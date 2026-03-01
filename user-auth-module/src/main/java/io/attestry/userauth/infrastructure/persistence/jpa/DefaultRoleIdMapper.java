package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.membership.model.MembershipRole;

final class DefaultRoleIdMapper {

    private DefaultRoleIdMapper() {
    }

    static String map(MembershipRole role, GroupType groupType) {
        // v1 simplification phase-1:
        // membership 생성 기본 role은 tenant 공통 baseline(TENANT_STAFF)으로 고정.
        // 운영/작업 권한은 이후 assignment/template로 부여한다.
        return "role-tenant-staff";
    }
}

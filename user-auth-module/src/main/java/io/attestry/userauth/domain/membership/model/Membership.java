package io.attestry.userauth.domain.membership.model;

import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.organization.model.TenantStatus;

public record Membership(
    String membershipId,
    String userId,
    String groupId,
    String tenantId,
    GroupType groupType,
    MembershipRole role,
    MembershipStatus status,
    GroupStatus groupStatus,
    TenantStatus tenantStatus
) {
    public boolean isActive() {
        return status == MembershipStatus.ACTIVE
            && groupStatus == GroupStatus.ACTIVE
            && tenantStatus == TenantStatus.ACTIVE;
    }
}

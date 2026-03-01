package io.attestry.userauth.application.dto.view;

import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import java.util.List;
import java.util.Set;

public record MembershipView(
    String membershipId,
    String tenantId,
    String groupId,
    GroupType groupType,
    List<String> roleCodes,
    MembershipStatus status,
    Set<String> effectiveScopes
) {
}

package io.attestry.userauth.application.dto;

import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.auth.model.Scope;
import java.util.Set;

public record MembershipView(
    String membershipId,
    String tenantId,
    String groupId,
    GroupType groupType,
    MembershipRole role,
    MembershipStatus status,
    Set<Scope> effectiveScopes
) {
}

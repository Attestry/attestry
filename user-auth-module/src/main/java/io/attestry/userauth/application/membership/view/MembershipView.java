package io.attestry.userauth.application.membership.view;

import io.attestry.userauth.domain.tenant.model.TenantType;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import java.util.List;
import java.util.Set;

public record MembershipView(
    String membershipId,
    String tenantId,
    String tenantName,
    TenantType groupType,
    List<String> roleCodes,
    MembershipStatus status,
    Set<String> effectiveScopes
) {
}

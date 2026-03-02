package io.attestry.userauth.domain.membership.event;

import java.time.Instant;

public record RoleAssignmentAuditedEvent(
    String actorUserId,
    String actorTenantId,
    String targetMembershipId,
    String beforeRole,
    String afterRole,
    String decisionSource,
    boolean allowed,
    String reasonCode,
    Instant requestedAt,
    Instant decidedAt
) {
}

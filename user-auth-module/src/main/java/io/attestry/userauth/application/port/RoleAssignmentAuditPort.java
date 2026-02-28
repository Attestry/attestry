package io.attestry.userauth.application.port;

import java.time.Instant;

public interface RoleAssignmentAuditPort {
    void log(
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
    );
}

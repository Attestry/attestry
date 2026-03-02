package io.attestry.userauth.domain.membership.event;

import java.time.Instant;

public record TemplatePermissionMutatedEvent(
    String actorUserId,
    String tenantId,
    String membershipId,
    String templateCode,
    Action action,
    Instant occurredAt,
    String reason
) {
    public enum Action {
        APPLY,
        REVOKE
    }
}

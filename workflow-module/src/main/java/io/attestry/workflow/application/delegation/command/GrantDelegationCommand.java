package io.attestry.workflow.application.delegation.command;

import java.time.Instant;

public record GrantDelegationCommand(
    String partnerLinkId,
    String resourceType,
    String resourceId, // passportId
    String permissionCode,
    Instant expiresAt,
    String note
) {
}

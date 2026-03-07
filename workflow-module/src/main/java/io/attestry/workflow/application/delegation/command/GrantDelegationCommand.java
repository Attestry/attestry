package io.attestry.workflow.application.delegation.command;

import java.time.Instant;

public record GrantDelegationCommand(
    String partnerLinkId,
    String targetTenantId,
    String resourceType,
    String resourceId, // passportId
    String permissionCode,
    Instant expiresAt,
    String note
) {
}

package io.attestry.workflow.application.delegation.result;

import java.time.Instant;

public record DelegationResult(
    String delegationId,
    String partnerLinkId,
    String sourceTenantId,
    String targetTenantId,
    String resourceType,
    String resourceId,
    String permissionCode,
    String status,
    Instant expiresAt,
    String reason
) {
}

package io.attestry.workflow.interfaces.delegation.dto.response;

import io.attestry.workflow.application.delegation.result.DelegationResult;
import java.time.Instant;

public record DelegationResponse(
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
    public static DelegationResponse from(DelegationResult result) {
        return new DelegationResponse(
            result.delegationId(),
            result.partnerLinkId(),
            result.sourceTenantId(),
            result.targetTenantId(),
            result.resourceType(),
            result.resourceId(),
            result.permissionCode(),
            result.status(),
            result.expiresAt(),
            result.reason()
        );
    }
}

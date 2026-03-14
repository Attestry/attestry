package io.attestry.workflow.interfaces.delegation.dto.request;

import java.time.Instant;

public record GrantDelegationRequest(
    String partnerLinkId,
    String resourceType,
    String resourceId,
    String permissionCode,
    Instant expiresAt,
    String note
) {
}

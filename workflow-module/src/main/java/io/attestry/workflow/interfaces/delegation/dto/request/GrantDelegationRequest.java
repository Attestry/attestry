package io.attestry.workflow.interfaces.delegation.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record GrantDelegationRequest(
    @NotBlank(message = "Partner link ID is required")
    String partnerLinkId,
    @NotBlank(message = "Resource type is required")
    String resourceType,
    @NotBlank(message = "Resource ID is required")
    String resourceId,
    @NotBlank(message = "Permission code is required")
    String permissionCode,
    Instant expiresAt,
    String note
) {
}

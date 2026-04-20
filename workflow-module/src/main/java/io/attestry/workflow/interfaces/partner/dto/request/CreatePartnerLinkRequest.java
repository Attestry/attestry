package io.attestry.workflow.interfaces.partner.dto.request;

import io.attestry.workflow.domain.partner.model.PartnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreatePartnerLinkRequest(
        @NotBlank(message = "Target tenant ID is required")
        String targetTenantId,
        @NotNull(message = "Partner type is required")
        PartnerType partnerType,
        Instant proposedExpiresAt,
        String message
) {
}

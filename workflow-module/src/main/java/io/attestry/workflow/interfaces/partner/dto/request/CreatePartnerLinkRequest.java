package io.attestry.workflow.interfaces.partner.dto.request;

import io.attestry.workflow.domain.partner.model.PartnerType;

import java.time.Instant;

public record CreatePartnerLinkRequest(
        String targetTenantId,
        String partnerTenantId,
        PartnerType partnerType,
        Instant proposedExpiresAt,
        String message
) {
    public String resolvedTargetTenantId() {
        if (targetTenantId != null && !targetTenantId.isBlank()) {
            return targetTenantId;
        }
        return partnerTenantId;
    }
}
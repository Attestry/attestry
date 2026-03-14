package io.attestry.workflow.interfaces.partner.dto.request;

import io.attestry.workflow.domain.partner.model.PartnerType;

import java.time.Instant;

public record CreatePartnerLinkRequest(
        String targetTenantId,
        PartnerType partnerType,
        Instant proposedExpiresAt,
        String message
) {
}

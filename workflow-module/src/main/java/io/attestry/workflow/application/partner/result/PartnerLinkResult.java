package io.attestry.workflow.application.partner.result;

import java.time.Instant;

public record PartnerLinkResult(
        String partnerLinkId,
        String sourceTenantId,
        String sourceTenantName,
        String targetTenantId,
        String targetTenantName,
        String partnerType,
        String status,
        String reason,
        Instant expiresAt) {
}

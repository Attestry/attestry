package io.attestry.workflow.interfaces.partner.dto.response;

import io.attestry.workflow.application.partner.result.PartnerLinkResult;
import java.time.Instant;

public record PartnerLinkResponse(
        String partnerLinkId,
        String sourceTenantId,
        String sourceTenantName,
        String sourceType,
        String targetTenantId,
        String targetTenantName,
        String partnerType,
        String status,
        String reason,
        Instant expiresAt) {
    public static PartnerLinkResponse from(PartnerLinkResult result) {
        return new PartnerLinkResponse(
                result.partnerLinkId(),
                result.sourceTenantId(),
                result.sourceTenantName(),
                result.sourceType(),
                result.targetTenantId(),
                result.targetTenantName(),
                result.partnerType(),
                result.status(),
                result.reason(),
                result.expiresAt());
    }
}
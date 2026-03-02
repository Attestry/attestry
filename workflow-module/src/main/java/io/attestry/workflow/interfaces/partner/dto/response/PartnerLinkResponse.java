package io.attestry.workflow.interfaces.partner.dto.response;

import io.attestry.workflow.application.partner.result.PartnerLinkResult;
import io.attestry.workflow.interfaces.partner.PartnerLinkHttp;

public record PartnerLinkResponse(
        String partnerLinkId,
        String sourceTenantId,
        String targetTenantId,
        String partnerType,
        String status,
        String reason
) {
    public static PartnerLinkResponse from(PartnerLinkResult result) {
        return new PartnerLinkResponse(
                result.partnerLinkId(),
                result.sourceTenantId(),
                result.targetTenantId(),
                result.partnerType(),
                result.status(),
                result.reason()
        );
    }
}
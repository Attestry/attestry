package io.attestry.workflow.application.partner.result;

public record PartnerLinkResult(
    String partnerLinkId,
    String sourceTenantId,
    String targetTenantId,
    String partnerType,
    String status,
    String reason
) {
}

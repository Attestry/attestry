package io.attestry.workflow.application.partner.result;

public record PartnerLinkResult(
    String partnerLinkId,
    String brandTenantId,
    String partnerTenantId,
    String partnerType,
    String status,
    String reason
) {
}

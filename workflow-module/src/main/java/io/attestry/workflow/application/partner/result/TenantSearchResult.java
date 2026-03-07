package io.attestry.workflow.application.partner.result;

public record TenantSearchResult(
    String tenantId,
    String name,
    String region,
    String type
) {
}

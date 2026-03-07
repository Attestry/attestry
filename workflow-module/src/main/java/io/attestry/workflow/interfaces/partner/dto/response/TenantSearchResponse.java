package io.attestry.workflow.interfaces.partner.dto.response;

import io.attestry.workflow.application.partner.result.TenantSearchResult;

public record TenantSearchResponse(
    String tenantId,
    String name,
    String region,
    String type
) {
    public static TenantSearchResponse from(TenantSearchResult result) {
        return new TenantSearchResponse(
            result.tenantId(),
            result.name(),
            result.region(),
            result.type()
        );
    }
}

package io.attestry.userauth.interfaces.tenant.dto.response;

import io.attestry.userauth.application.tenant.query.TenantView;

public record TenantResponse(
        String tenantId,
        String name,
        String region,
        String address,
        String type,
        String status) {

    public static TenantResponse from(TenantView result) {
        return new TenantResponse(
                result.tenantId(),
                result.name(),
                result.region(),
                result.address(),
                result.type(),
                result.status());
    }
}

package io.attestry.userauth.interfaces.tenant.dto.response;

import io.attestry.userauth.application.dto.result.TenantResult;

public record TenantResponse(
        String tenantId,
        String name,
        String region,
        String address,
        String type,
        String status) {

    public static TenantResponse from(TenantResult result) {
        return new TenantResponse(
                result.tenantId(),
                result.name(),
                result.region(),
                result.address(),
                result.type(),
                result.status());
    }
}

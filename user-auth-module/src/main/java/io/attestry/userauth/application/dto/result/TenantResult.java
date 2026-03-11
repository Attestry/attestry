package io.attestry.userauth.application.dto.result;

public record TenantResult(
        String tenantId,
        String name,
        String region,
        String address,
        String type,
        String status) {
}

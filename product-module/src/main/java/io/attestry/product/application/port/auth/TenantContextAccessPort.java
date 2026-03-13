package io.attestry.product.application.port.auth;

import io.attestry.product.application.dto.command.ProductTenantType;

public interface TenantContextAccessPort {

    void assertActiveTenantMembership(String actorUserId, String tenantId, ProductTenantType tenantType);
}

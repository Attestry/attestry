package io.attestry.product.application.port.auth;

import io.attestry.product.application.common.ProductTenantType;

public interface TenantContextAccessPort {

    void assertActiveTenantMembership(String actorUserId, String tenantId, ProductTenantType tenantType);
}

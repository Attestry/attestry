package io.attestry.product.application.command.internal;

import io.attestry.product.application.common.ProductActor;
import io.attestry.product.application.common.ProductTenantType;
import io.attestry.product.application.port.auth.ProductAuthorizationPort;
import io.attestry.product.application.port.auth.TenantContextAccessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductVoidAccessPolicy {

    private final TenantContextAccessPort tenantContextAccessPort;
    private final ProductAuthorizationPort productAuthorizationPort;

    public void assertVoidAllowed(ProductActor actor, String tenantId, String passportId) {
        tenantContextAccessPort.assertActiveTenantMembership(actor.userId(), tenantId, ProductTenantType.BRAND);
        productAuthorizationPort.assertBrandVoidAllowed(actor, tenantId, passportId);
    }
}

package io.attestry.product.application.policy;

import io.attestry.product.application.common.ProductActor;
import io.attestry.product.application.common.ProductTenantType;
import io.attestry.product.application.port.auth.ProductAuthorizationPort;
import io.attestry.product.application.port.auth.TenantContextAccessPort;
import io.attestry.product.application.command.support.LedgerActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductMintAccessPolicy {

    private static final String PLATFORM_ADMIN_ROLE = "ADMIN";
    private static final String BRAND_ROLE = "BRAND";

    private final TenantContextAccessPort tenantContextAccessPort;
    private final ProductAuthorizationPort productAuthorizationPort;

    public void assertMintAllowed(ProductActor actor, String tenantId, String serialNumber) {
        if (actor.platformAdmin()) {
            return;
        }
        tenantContextAccessPort.assertActiveTenantMembership(actor.userId(), tenantId, ProductTenantType.BRAND);
        productAuthorizationPort.assertBrandMintAllowed(actor, tenantId, serialNumber);
    }

    public LedgerActor resolveLedgerActor(ProductActor actor, String tenantId) {
        if (actor.platformAdmin()) {
            return new LedgerActor(PLATFORM_ADMIN_ROLE, actor.userId());
        }
        return new LedgerActor(BRAND_ROLE, tenantId);
    }
}

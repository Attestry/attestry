package io.attestry.product.application.policy;

import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.dto.command.ProductTenantType;
import io.attestry.product.application.port.auth.ProductAuthorizationPort;
import io.attestry.product.application.port.auth.TenantContextAccessPort;
import io.attestry.product.application.command.dto.LedgerActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductMintAccessPolicy {

    private static final String PLATFORM_ADMIN_ROLE = "ADMIN";
    private static final String BRAND_ROLE = "BRAND";
    private static final String BATCH_RESOURCE_REF = "batch";

    private final TenantContextAccessPort tenantContextAccessPort;
    private final ProductAuthorizationPort productAuthorizationPort;

    public void assertSingleMintAllowed(ProductActor actor, String tenantId, String serialNumber) {
        if (actor.platformAdmin()) {
            return;
        }
        tenantContextAccessPort.assertActiveTenantMembership(actor.userId(), tenantId, ProductTenantType.BRAND);
        productAuthorizationPort.assertBrandMintAllowed(actor, tenantId, serialNumber);
    }

    public void assertBatchMintAllowed(ProductActor actor, String tenantId) {
        tenantContextAccessPort.assertActiveTenantMembership(actor.userId(), tenantId, ProductTenantType.BRAND);
        productAuthorizationPort.assertBrandMintAllowed(actor, tenantId, BATCH_RESOURCE_REF);
    }

    public LedgerActor resolveLedgerActor(ProductActor actor, String tenantId) {
        if (actor.platformAdmin()) {
            return new LedgerActor(PLATFORM_ADMIN_ROLE, actor.userId());
        }
        return new LedgerActor(BRAND_ROLE, tenantId);
    }
}

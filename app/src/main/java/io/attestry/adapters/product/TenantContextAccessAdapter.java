package io.attestry.adapters.product;

import io.attestry.product.application.dto.command.ProductTenantType;
import io.attestry.product.application.port.auth.TenantContextAccessPort;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.userauth.contract.membership.TenantMembershipCheckPort;
import org.springframework.stereotype.Component;

@Component
public class TenantContextAccessAdapter implements TenantContextAccessPort {

    private final TenantMembershipCheckPort tenantMembershipCheckPort;

    public TenantContextAccessAdapter(TenantMembershipCheckPort tenantMembershipCheckPort) {
        this.tenantMembershipCheckPort = tenantMembershipCheckPort;
    }

    @Override
    public void assertActiveTenantMembership(String actorUserId, String tenantId, ProductTenantType tenantType) {
        TenantMembershipCheckPort.MembershipCheckResult result = tenantMembershipCheckPort.checkActiveMembership(
            actorUserId,
            tenantId,
            tenantType.name()
        );
        if (!result.active()) {
            throw missingTenantContext(tenantType);
        }
    }

    private ProductDomainException missingTenantContext(ProductTenantType tenantType) {
        return new ProductDomainException(
            ProductErrorCode.MINT_CONTEXT_NOT_FOUND,
            "Active " + tenantType.name() + " membership context is required"
        );
    }
}

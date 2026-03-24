package io.attestry.workflow.application.shipment.policy;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.shipment.model.Shipment;
import org.springframework.stereotype.Component;

@Component
public class ShipmentQueryAccessPolicy {

    private final WorkflowAuthorizationSupport authorizationSupport;

    public ShipmentQueryAccessPolicy(WorkflowAuthorizationSupport authorizationSupport) {
        this.authorizationSupport = authorizationSupport;
    }

    public String assertPassportListAccess(WorkflowActorContext principal, String passportId) {
        String tenantId = principal.tenantId();
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "shipment:list:" + passportId);
        return tenantId;
    }

    public String assertTenantShipmentListAccess(WorkflowActorContext principal) {
        String tenantId = principal.tenantId();
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.TENANT_READ_ONLY, "shipment:list-all");
        return tenantId;
    }

    public String assertReleaseCandidateAccess(WorkflowActorContext principal) {
        String tenantId = principal.tenantId();
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.TENANT_READ_ONLY, "shipment:release-candidates");
        return tenantId;
    }

    public void assertShipmentDetailAccess(WorkflowActorContext principal, Shipment shipment) {
        String tenantId = principal.tenantId();
        authorizationSupport.assertTenantContext(principal, tenantId);
        if (!tenantId.equals(shipment.tenantId())) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.TENANT_ISOLATION_VIOLATION,
                "Cross-tenant shipment access denied"
            );
        }
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.TENANT_READ_ONLY, "shipment:view:" + shipment.shipmentId());
    }
}

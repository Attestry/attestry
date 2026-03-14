package io.attestry.workflow.domain.shipment.policy;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.shipment.model.ShipmentStatus;
import org.springframework.stereotype.Component;

@Component
public class ShipmentReturnPolicy {

    public void assertReturnable(ShipmentReturnContext context) {
        assertShipmentBelongsToTenant(context);
        assertShipmentReleased(context);
    }

    private void assertShipmentBelongsToTenant(ShipmentReturnContext context) {
        if (!context.tenantId().equals(context.shipment().tenantId())) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.TENANT_ISOLATION_VIOLATION,
                "Cross-tenant shipment access denied"
            );
        }
    }

    private void assertShipmentReleased(ShipmentReturnContext context) {
        if (context.shipment().status() != ShipmentStatus.RELEASED) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Only RELEASED shipment can be returned"
            );
        }
    }

    public record ShipmentReturnContext(
        String tenantId,
        Shipment shipment
    ) {
    }
}

package io.attestry.workflow.domain.shipment.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.shipment.model.ShipmentStatus;
import io.attestry.workflow.domain.shipment.policy.ShipmentReturnPolicy.ShipmentReturnContext;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ShipmentReturnPolicyTest {

    private final ShipmentReturnPolicy policy = new ShipmentReturnPolicy();

    @Test
    void return_success() {
        Shipment shipment = new Shipment(
            "ship1", "tenant1", "passport1", 1, ShipmentStatus.RELEASED,
            Instant.now(), "user1", "tenant1", "evidence1",
            null, null, null, Instant.now()
        );
        ShipmentReturnContext context = new ShipmentReturnContext("tenant1", shipment);
        assertDoesNotThrow(() -> policy.assertReturnable(context));
    }

    @Test
    void return_failsTenantMismatch() {
        Shipment shipment = new Shipment(
            "ship1", "tenant1", "passport1", 1, ShipmentStatus.RELEASED,
            Instant.now(), "user1", "tenant1", "evidence1",
            null, null, null, Instant.now()
        );
        ShipmentReturnContext context = new ShipmentReturnContext("tenant2", shipment);
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertReturnable(context)
        );
        assertEquals(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, ex.getErrorCode());
    }

    @Test
    void return_failsNotReleased() {
        Shipment shipment = new Shipment(
            "ship1", "tenant1", "passport1", 1, ShipmentStatus.RETURNED,
            Instant.now(), "user1", "tenant1", "evidence1",
            Instant.now(), "user2", "returnEvidence1", Instant.now()
        );
        ShipmentReturnContext context = new ShipmentReturnContext("tenant1", shipment);
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertReturnable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }
}

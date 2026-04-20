package io.attestry.workflow.domain.shipment.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.passport.model.WorkflowAssetState;
import io.attestry.workflow.domain.passport.model.WorkflowRiskFlag;
import io.attestry.workflow.domain.shipment.policy.ShipmentReleasePolicy.ShipmentReleaseContext;
import org.junit.jupiter.api.Test;

class ShipmentReleasePolicyTest {

    private final ShipmentReleasePolicy policy = new ShipmentReleasePolicy();

    @Test
    void release_success() {
        ShipmentReleaseContext context = new ShipmentReleaseContext(
            "tenant1", "tenant1", WorkflowAssetState.ACTIVE, WorkflowRiskFlag.NONE, false
        );
        assertDoesNotThrow(() -> policy.assertReleasable(context));
    }

    @Test
    void release_failsTenantMismatch() {
        ShipmentReleaseContext context = new ShipmentReleaseContext(
            "tenant1", "tenant2", WorkflowAssetState.ACTIVE, WorkflowRiskFlag.NONE, false
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertReleasable(context)
        );
        assertEquals(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, ex.getErrorCode());
    }

    @Test
    void release_failsNotActive() {
        ShipmentReleaseContext context = new ShipmentReleaseContext(
            "tenant1", "tenant1", WorkflowAssetState.VOIDED, WorkflowRiskFlag.NONE, false
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertReleasable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void release_failsRiskFlagged() {
        ShipmentReleaseContext context = new ShipmentReleaseContext(
            "tenant1", "tenant1", WorkflowAssetState.ACTIVE, WorkflowRiskFlag.FLAGGED, false
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertReleasable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void release_failsAlreadyReleased() {
        ShipmentReleaseContext context = new ShipmentReleaseContext(
            "tenant1", "tenant1", WorkflowAssetState.ACTIVE, WorkflowRiskFlag.NONE, true
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertReleasable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }
}

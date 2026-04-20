package io.attestry.workflow.domain.transfer.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.passport.model.WorkflowAssetState;
import io.attestry.workflow.domain.passport.model.WorkflowRiskFlag;
import io.attestry.workflow.domain.transfer.policy.TransferCreatePolicy.TransferCreateContext;
import org.junit.jupiter.api.Test;

class TransferCreatePolicyTest {

    private final TransferCreatePolicy policy = new TransferCreatePolicy();

    // ── C2C tests ──

    @Test
    void c2c_success() {
        TransferCreateContext context = new TransferCreateContext(
            "owner1", "tenant1", WorkflowAssetState.ACTIVE, WorkflowRiskFlag.NONE,
            "tenant1", "owner1", false, false
        );
        assertDoesNotThrow(() -> policy.assertC2CCreatable(context));
    }

    @Test
    void c2c_failsWhenPassportNull() {
        TransferCreateContext context = new TransferCreateContext(
            "owner1", "tenant1", (WorkflowAssetState) null, WorkflowRiskFlag.NONE,
            "tenant1", "owner1", false, false
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertC2CCreatable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void c2c_failsWhenPassportNotActive() {
        TransferCreateContext context = new TransferCreateContext(
            "owner1", "tenant1", WorkflowAssetState.VOIDED, WorkflowRiskFlag.NONE,
            "tenant1", "owner1", false, false
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertC2CCreatable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void c2c_failsWhenRiskFlagged() {
        TransferCreateContext context = new TransferCreateContext(
            "owner1", "tenant1", WorkflowAssetState.ACTIVE, WorkflowRiskFlag.FLAGGED,
            "tenant1", "owner1", false, false
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertC2CCreatable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void c2c_failsWhenOwnerNull() {
        TransferCreateContext context = new TransferCreateContext(
            "owner1", "tenant1", WorkflowAssetState.ACTIVE, WorkflowRiskFlag.NONE,
            "tenant1", null, false, false
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertC2CCreatable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void c2c_failsWhenNotOwner() {
        TransferCreateContext context = new TransferCreateContext(
            "actor1", "tenant1", WorkflowAssetState.ACTIVE, WorkflowRiskFlag.NONE,
            "tenant1", "owner1", false, false
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertC2CCreatable(context)
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }

    @Test
    void c2c_failsWhenPendingExists() {
        TransferCreateContext context = new TransferCreateContext(
            "owner1", "tenant1", WorkflowAssetState.ACTIVE, WorkflowRiskFlag.NONE,
            "tenant1", "owner1", false, true
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertC2CCreatable(context)
        );
        assertEquals(WorkflowErrorCode.TRANSFER_ALREADY_PENDING, ex.getErrorCode());
    }

    // ── B2C tests ──

    @Test
    void b2c_success() {
        TransferCreateContext context = new TransferCreateContext(
            "actor1", "tenant1", WorkflowAssetState.ACTIVE, WorkflowRiskFlag.NONE,
            "tenant1", null, true, false
        );
        assertDoesNotThrow(() -> policy.assertB2CCreatable(context));
    }

    @Test
    void b2c_failsWhenOwnerExists() {
        TransferCreateContext context = new TransferCreateContext(
            "actor1", "tenant1", WorkflowAssetState.ACTIVE, WorkflowRiskFlag.NONE,
            "tenant1", "existingOwner", true, false
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertB2CCreatable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void b2c_failsWhenNoRetailPermission() {
        TransferCreateContext context = new TransferCreateContext(
            "actor1", "tenant1", WorkflowAssetState.ACTIVE, WorkflowRiskFlag.NONE,
            "tenant1", null, false, false
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertB2CCreatable(context)
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }

    @Test
    void b2c_failsWhenPendingExists() {
        TransferCreateContext context = new TransferCreateContext(
            "actor1", "tenant1", WorkflowAssetState.ACTIVE, WorkflowRiskFlag.NONE,
            "tenant1", null, true, true
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertB2CCreatable(context)
        );
        assertEquals(WorkflowErrorCode.TRANSFER_ALREADY_PENDING, ex.getErrorCode());
    }
}

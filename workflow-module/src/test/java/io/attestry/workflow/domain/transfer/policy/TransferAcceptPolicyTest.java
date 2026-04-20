package io.attestry.workflow.domain.transfer.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.passport.model.WorkflowRiskFlag;
import io.attestry.workflow.domain.transfer.policy.TransferAcceptPolicy.TransferAcceptContext;
import org.junit.jupiter.api.Test;

class TransferAcceptPolicyTest {

    private final TransferAcceptPolicy policy = new TransferAcceptPolicy();

    @Test
    void accept_c2c_success() {
        TransferAcceptContext context = new TransferAcceptContext(
            true, WorkflowRiskFlag.NONE, "owner1", "owner1"
        );
        assertDoesNotThrow(() -> policy.assertAcceptable(context));
    }

    @Test
    void accept_b2c_success() {
        TransferAcceptContext context = new TransferAcceptContext(
            false, WorkflowRiskFlag.NONE, null, null
        );
        assertDoesNotThrow(() -> policy.assertAcceptable(context));
    }

    @Test
    void accept_failsWhenRiskFlagged() {
        TransferAcceptContext context = new TransferAcceptContext(
            true, WorkflowRiskFlag.FLAGGED, "owner1", "owner1"
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertAcceptable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void accept_c2c_failsWhenOwnerNull() {
        TransferAcceptContext context = new TransferAcceptContext(
            true, WorkflowRiskFlag.NONE, null, "owner1"
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertAcceptable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void accept_c2c_failsWhenOwnerChanged() {
        TransferAcceptContext context = new TransferAcceptContext(
            true, WorkflowRiskFlag.NONE, "newOwner", "originalOwner"
        );
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertAcceptable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }
}

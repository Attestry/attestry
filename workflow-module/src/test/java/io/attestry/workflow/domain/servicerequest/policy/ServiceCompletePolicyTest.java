package io.attestry.workflow.domain.servicerequest.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.policy.ServiceCompletePolicy.ServiceCompleteContext;
import org.junit.jupiter.api.Test;

class ServiceCompletePolicyTest {

    private final ServiceCompletePolicy policy = new ServiceCompletePolicy();

    @Test
    void assertCompletable_validContext_succeeds() {
        ServiceCompleteContext context = new ServiceCompleteContext("ACTIVE", "NONE", true);
        assertDoesNotThrow(() -> policy.assertCompletable(context));
    }

    @Test
    void assertCompletable_noPermission_throws() {
        ServiceCompleteContext context = new ServiceCompleteContext("ACTIVE", "NONE", false);
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertCompletable(context)
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }

    @Test
    void assertCompletable_assetNotActive_throws() {
        ServiceCompleteContext context = new ServiceCompleteContext("VOIDED", "NONE", true);
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertCompletable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void assertCompletable_riskFlagged_throws() {
        ServiceCompleteContext context = new ServiceCompleteContext("ACTIVE", "FLAGGED", true);
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertCompletable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }
}

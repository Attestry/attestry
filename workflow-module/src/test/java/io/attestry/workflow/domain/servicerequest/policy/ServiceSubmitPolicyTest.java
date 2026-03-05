package io.attestry.workflow.domain.servicerequest.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.policy.ServiceSubmitPolicy.ServiceSubmitContext;
import org.junit.jupiter.api.Test;

class ServiceSubmitPolicyTest {

    private final ServiceSubmitPolicy policy = new ServiceSubmitPolicy();

    @Test
    void assertSubmittable_validContext_succeeds() {
        ServiceSubmitContext context = new ServiceSubmitContext("ACTIVE", "NONE", true, false);
        assertDoesNotThrow(() -> policy.assertSubmittable(context));
    }

    @Test
    void assertSubmittable_noPermission_throws() {
        ServiceSubmitContext context = new ServiceSubmitContext("ACTIVE", "NONE", false, false);
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertSubmittable(context)
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }

    @Test
    void assertSubmittable_assetNotActive_throws() {
        ServiceSubmitContext context = new ServiceSubmitContext("VOIDED", "NONE", true, false);
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertSubmittable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void assertSubmittable_riskFlagged_throws() {
        ServiceSubmitContext context = new ServiceSubmitContext("ACTIVE", "FLAGGED", true, false);
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertSubmittable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void assertSubmittable_duplicateSubmission_throws() {
        ServiceSubmitContext context = new ServiceSubmitContext("ACTIVE", "NONE", true, true);
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertSubmittable(context)
        );
        assertEquals(WorkflowErrorCode.SERVICE_REQUEST_ALREADY_SUBMITTED, ex.getErrorCode());
    }
}

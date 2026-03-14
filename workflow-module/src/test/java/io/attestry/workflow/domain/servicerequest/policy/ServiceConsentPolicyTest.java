package io.attestry.workflow.domain.servicerequest.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.policy.ServiceConsentPolicy.ServiceConsentContext;
import org.junit.jupiter.api.Test;

class ServiceConsentPolicyTest {

    private final ServiceConsentPolicy policy = new ServiceConsentPolicy();

    @Test
    void assertConsentGrantable_validContext_succeeds() {
        ServiceConsentContext context = new ServiceConsentContext("owner1", "owner1", "ACTIVE", "NONE");
        assertDoesNotThrow(() -> policy.assertConsentGrantable(context));
    }

    @Test
    void assertConsentGrantable_assetNotActive_throws() {
        ServiceConsentContext context = new ServiceConsentContext("owner1", "owner1", "VOIDED", "NONE");
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertConsentGrantable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void assertConsentGrantable_riskFlagged_throws() {
        ServiceConsentContext context = new ServiceConsentContext("owner1", "owner1", "ACTIVE", "FLAGGED");
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertConsentGrantable(context)
        );
        assertEquals(WorkflowErrorCode.INVALID_STATE, ex.getErrorCode());
    }

    @Test
    void assertConsentGrantable_ownerMismatch_throws() {
        ServiceConsentContext context = new ServiceConsentContext("otherUser", "owner1", "ACTIVE", "NONE");
        WorkflowDomainException ex = assertThrows(WorkflowDomainException.class, () ->
            policy.assertConsentGrantable(context)
        );
        assertEquals(WorkflowErrorCode.FORBIDDEN_SCOPE, ex.getErrorCode());
    }
}

package io.attestry.workflow.domain.servicerequest.policy;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ServiceConsentPolicy {

    public void assertConsentGrantable(ServiceConsentContext context) {
        assertPassportActive(context);
        assertNoRiskFlag(context);
        assertOwnerMatch(context);
    }

    private void assertPassportActive(ServiceConsentContext context) {
        if (!"ACTIVE".equals(context.assetState())) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Passport asset must be ACTIVE"
            );
        }
    }

    private void assertNoRiskFlag(ServiceConsentContext context) {
        if (!"NONE".equals(context.riskFlag())) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Risk flagged passport cannot grant service consent"
            );
        }
    }

    private void assertOwnerMatch(ServiceConsentContext context) {
        if (!context.requestingUserId().equals(context.currentOwnerId())) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.FORBIDDEN_SCOPE,
                "Only the passport owner can grant service consent"
            );
        }
    }

    public record ServiceConsentContext(
        String requestingUserId,
        String currentOwnerId,
        String assetState,
        String riskFlag
    ) {
    }
}

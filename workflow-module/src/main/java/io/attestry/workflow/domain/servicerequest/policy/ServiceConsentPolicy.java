package io.attestry.workflow.domain.servicerequest.policy;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.passport.model.WorkflowAssetState;
import io.attestry.workflow.domain.passport.model.WorkflowRiskFlag;
import org.springframework.stereotype.Component;

@Component
public class ServiceConsentPolicy {

    public void assertConsentGrantable(ServiceConsentContext context) {
        assertPassportActive(context);
        assertNoRiskFlag(context);
        assertOwnerMatch(context);
    }

    private void assertPassportActive(ServiceConsentContext context) {
        if (context.assetState() != WorkflowAssetState.ACTIVE) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Passport asset must be ACTIVE"
            );
        }
    }

    private void assertNoRiskFlag(ServiceConsentContext context) {
        if (context.riskFlag() != WorkflowRiskFlag.NONE) {
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
        WorkflowAssetState assetState,
        WorkflowRiskFlag riskFlag
    ) {
        public ServiceConsentContext(
            String requestingUserId,
            String currentOwnerId,
            String assetState,
            String riskFlag
        ) {
            this(
                requestingUserId,
                currentOwnerId,
                WorkflowAssetState.from(assetState),
                WorkflowRiskFlag.from(riskFlag)
            );
        }
    }
}

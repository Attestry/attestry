package io.attestry.workflow.domain.servicerequest.policy;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ServiceCompletePolicy {

    public void assertCompletable(ServiceCompleteContext context) {
        assertPassportActive(context);
        assertNoRiskFlag(context);
        assertHasServicePermission(context);
    }

    private void assertPassportActive(ServiceCompleteContext context) {
        if (!"ACTIVE".equals(context.assetState())) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Passport asset must be ACTIVE"
            );
        }
    }

    private void assertNoRiskFlag(ServiceCompleteContext context) {
        if (!"NONE".equals(context.riskFlag())) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Risk flagged passport cannot complete service"
            );
        }
    }

    private void assertHasServicePermission(ServiceCompleteContext context) {
        if (!context.hasServiceRepairPermission()) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.FORBIDDEN_SCOPE,
                "No active SERVICE_REPAIR permission for this passport"
            );
        }
    }

    public record ServiceCompleteContext(
        String assetState,
        String riskFlag,
        boolean hasServiceRepairPermission
    ) {
    }
}

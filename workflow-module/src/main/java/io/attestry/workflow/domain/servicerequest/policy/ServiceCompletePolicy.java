package io.attestry.workflow.domain.servicerequest.policy;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.passport.model.WorkflowAssetState;
import io.attestry.workflow.domain.passport.model.WorkflowRiskFlag;
import org.springframework.stereotype.Component;

@Component
public class ServiceCompletePolicy {

    public void assertCompletable(ServiceCompleteContext context) {
        assertPassportActive(context);
        assertNoRiskFlag(context);
        assertHasServicePermission(context);
    }

    private void assertPassportActive(ServiceCompleteContext context) {
        if (context.assetState() != WorkflowAssetState.ACTIVE) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Passport asset must be ACTIVE"
            );
        }
    }

    private void assertNoRiskFlag(ServiceCompleteContext context) {
        if (context.riskFlag() != WorkflowRiskFlag.NONE) {
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
        WorkflowAssetState assetState,

        WorkflowRiskFlag riskFlag,
        boolean hasServiceRepairPermission
    ) {
        public ServiceCompleteContext(
            String assetState,
            String riskFlag,
            boolean hasServiceRepairPermission
        ) {
            this(
                WorkflowAssetState.from(assetState),
                WorkflowRiskFlag.from(riskFlag),
                hasServiceRepairPermission
            );
        }
    }
}

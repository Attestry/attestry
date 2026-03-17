package io.attestry.workflow.domain.servicerequest.policy;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.passport.model.WorkflowAssetState;
import io.attestry.workflow.domain.passport.model.WorkflowRiskFlag;
import org.springframework.stereotype.Component;

@Component
public class ServiceSubmitPolicy {

    public void assertSubmittable(ServiceSubmitContext context) {
        assertPassportActive(context);
        assertNoRiskFlag(context);
        assertHasServiceRepairPermission(context);
        assertNoOpenRequest(context);
    }

    private void assertPassportActive(ServiceSubmitContext context) {
        if (context.assetState() != WorkflowAssetState.ACTIVE) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Passport asset must be ACTIVE"
            );
        }
    }

    private void assertNoRiskFlag(ServiceSubmitContext context) {
        if (context.riskFlag() != WorkflowRiskFlag.NONE) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Risk flagged passport cannot be submitted for service"
            );
        }
    }

    private void assertHasServiceRepairPermission(ServiceSubmitContext context) {
        if (!context.hasActiveServiceRepairPermission()) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.FORBIDDEN_SCOPE,
                "No active SERVICE_REPAIR consent for this passport"
            );
        }
    }

    private void assertNoOpenRequest(ServiceSubmitContext context) {
        if (context.openRequestExists()) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.SERVICE_REQUEST_ALREADY_SUBMITTED,
                "이미 처리 중인 서비스 요청이 있습니다."
            );
        }
    }

    public record ServiceSubmitContext(
        WorkflowAssetState assetState,
        WorkflowRiskFlag riskFlag,
        boolean hasActiveServiceRepairPermission,
        boolean openRequestExists
    ) {
        public ServiceSubmitContext(
            String assetState,
            String riskFlag,
            boolean hasActiveServiceRepairPermission,
            boolean openRequestExists
        ) {
            this(
                WorkflowAssetState.from(assetState),
                WorkflowRiskFlag.from(riskFlag),
                hasActiveServiceRepairPermission,
                openRequestExists
            );
        }
    }
}

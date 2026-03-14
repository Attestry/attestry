package io.attestry.workflow.domain.shipment.policy;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.passport.model.WorkflowAssetState;
import io.attestry.workflow.domain.passport.model.WorkflowRiskFlag;
import org.springframework.stereotype.Component;

@Component
public class ShipmentReleasePolicy {

    public void assertReleasable(ShipmentReleaseContext context) {
        assertPassportBelongsToScope(context);
        assertPassportActive(context);
        assertNoRiskFlag(context);
        assertNotAlreadyReleased(context);
    }

    private void assertPassportBelongsToScope(ShipmentReleaseContext context) {
        if (!context.tenantId().equals(context.passportTenantId())) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.TENANT_ISOLATION_VIOLATION,
                "Passport does not belong to tenant"
            );
        }
    }

    private void assertPassportActive(ShipmentReleaseContext context) {
        if (context.assetState() != WorkflowAssetState.ACTIVE) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Passport asset must be ACTIVE"
            );
        }
    }

    private void assertNoRiskFlag(ShipmentReleaseContext context) {
        if (context.riskFlag() != WorkflowRiskFlag.NONE) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Risk flagged passport cannot be released"
            );
        }
    }

    private void assertNotAlreadyReleased(ShipmentReleaseContext context) {
        if (context.activeReleasedExists()) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Passport is already RELEASED"
            );
        }
    }

    public record ShipmentReleaseContext(
        String tenantId,
        String passportTenantId,
        WorkflowAssetState assetState,
        WorkflowRiskFlag riskFlag,
        boolean activeReleasedExists
    ) {
        public ShipmentReleaseContext(
            String tenantId,
            String passportTenantId,
            String assetState,
            String riskFlag,
            boolean activeReleasedExists
        ) {
            this(
                tenantId,
                passportTenantId,
                WorkflowAssetState.from(assetState),
                WorkflowRiskFlag.from(riskFlag),
                activeReleasedExists
            );
        }
    }
}

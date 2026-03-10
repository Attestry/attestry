package io.attestry.workflow.domain.transfer.policy;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import org.springframework.stereotype.Component;

@Component
public class TransferCreatePolicy {

    public void assertC2CCreatable(TransferCreateContext context) {
        assertPassportTransferable(context);
        assertOwnerMatch(context);
        assertNoPendingTransfer(context);
    }

    public void assertB2CCreatable(TransferCreateContext context) {
        assertPassportTransferable(context);
        assertNoCurrentOwner(context);
        assertRetailPermission(context);
        assertNoPendingTransfer(context);
    }

    private void assertPassportTransferable(TransferCreateContext context) {
        if (context.passportAssetState() == null) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport not found");
        }
        if (!"ACTIVE".equals(context.passportAssetState())) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_STATE, "Passport asset must be ACTIVE");
        }
        if (!"NONE".equals(context.passportRiskFlag())) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_STATE, "Risk flagged passport cannot be transferred");
        }
    }

    private void assertOwnerMatch(TransferCreateContext context) {
        if (context.currentOwnerId() == null) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport has no owner");
        }
        if (!context.actorUserId().equals(context.currentOwnerId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Only the current owner can create a C2C transfer");
        }
    }

    private void assertNoCurrentOwner(TransferCreateContext context) {
        if (context.currentOwnerId() != null) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_STATE, "Passport already has an owner; use C2C transfer");
        }
    }

    private void assertRetailPermission(TransferCreateContext context) {
        if (!context.hasRetailPermission()) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "No retail permission for this passport");
        }
    }

    private void assertNoPendingTransfer(TransferCreateContext context) {
        if (context.pendingTransferExists()) {
            throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_ALREADY_PENDING, "A pending transfer already exists for this passport");
        }
    }

    public record TransferCreateContext(
        String actorUserId,
        String requestTenantId,
        String passportAssetState,
        String passportRiskFlag,
        String passportTenantId,
        String currentOwnerId,
        boolean hasRetailPermission,
        boolean pendingTransferExists
    ) {
    }
}

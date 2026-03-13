package io.attestry.workflow.domain.transfer.policy;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.passport.model.WorkflowRiskFlag;
import org.springframework.stereotype.Component;

@Component
public class TransferAcceptPolicy {

    public void assertAcceptable(TransferAcceptContext context) {
        assertPassportSafe(context);
        if (context.isC2C()) {
            assertOwnershipUnchanged(context);
        }
    }

    private void assertPassportSafe(TransferAcceptContext context) {
        if (context.passportRiskFlag() != WorkflowRiskFlag.NONE) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_STATE, "Risk flagged passport cannot be transferred");
        }
    }

    private void assertOwnershipUnchanged(TransferAcceptContext context) {
        if (context.currentOwnerId() == null) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_STATE, "Passport ownership changed during transfer");
        }
        if (!context.currentOwnerId().equals(context.expectedFromOwnerId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_STATE, "Passport ownership changed during transfer");
        }
    }

    public record TransferAcceptContext(
        boolean isC2C,
        WorkflowRiskFlag passportRiskFlag,
        String currentOwnerId,
        String expectedFromOwnerId
    ) {
        public TransferAcceptContext(
            boolean isC2C,
            String passportRiskFlag,
            String currentOwnerId,
            String expectedFromOwnerId
        ) {
            this(
                isC2C,
                WorkflowRiskFlag.from(passportRiskFlag),
                currentOwnerId,
                expectedFromOwnerId
            );
        }
    }
}

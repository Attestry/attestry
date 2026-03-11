package io.attestry.workflow.application.transfer;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.transfer.result.CancelTransferResult;
import io.attestry.workflow.application.usecase.TransferCancelUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferStatus;
import io.attestry.workflow.domain.transfer.model.TransferType;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TransferCancelService implements TransferCancelUseCase {

    private final TokenTransferRepository transferRepository;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final Clock clock;


    @Override
    @Transactional
    public CancelTransferResult cancel(AuthPrincipal principal, String transferId) {
        TokenTransfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.TRANSFER_NOT_FOUND, "Transfer not found"));

        if (transfer.status() != TransferStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.TRANSFER_INVALID_STATE, "Only PENDING transfer can be cancelled");
        }

        if (transfer.transferType() == TransferType.C2C) {
            if (!principal.userId().equals(transfer.createdByUserId())) {
                throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Only the transfer creator can cancel a C2C transfer");
            }
        } else {
            authorizationSupport.assertTenantContext(principal, transfer.tenantId());
            authorizationSupport.assertLivePermission(principal, transfer.tenantId(), PermissionCodes.RETAIL_TRANSFER_CREATE, "transfer:cancel:" + transferId);
        }

        Instant now = Instant.now(clock);
        TokenTransfer cancelled = transfer.cancel(principal.userId(), now);
        TokenTransfer saved = transferRepository.save(cancelled);

        return new CancelTransferResult(
            saved.transferId(), saved.passportId(),
            saved.status().name(), saved.cancelledAt()
        );
    }
}

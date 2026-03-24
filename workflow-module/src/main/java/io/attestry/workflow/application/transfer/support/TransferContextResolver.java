package io.attestry.workflow.application.transfer.support;

import io.attestry.workflow.application.port.transfer.TransferProductReadPort;
import io.attestry.workflow.application.port.transfer.TransferProductReadPort.TransferPassportState;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferType;
import io.attestry.workflow.domain.transfer.policy.TransferAcceptPolicy.TransferAcceptContext;
import io.attestry.workflow.domain.transfer.policy.TransferCreatePolicy.TransferCreateContext;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferContextResolver {

    private final TransferProductReadPort productReadPort;
    private final TokenTransferRepository transferRepository;

    public TransferCreateContext resolveCreateContext(String actorUserId, String requestTenantId, String passportId) {
        TransferPassportState state = productReadPort.findPassportState(passportId).orElse(null);
        String currentOwnerId = productReadPort.findCurrentOwnerId(passportId).orElse(null);
        boolean pendingExists = transferRepository.existsActivePendingByPassportId(passportId);
        boolean hasRetail = requestTenantId != null && productReadPort.hasRetailPermission(passportId, requestTenantId);

        return new TransferCreateContext(
            actorUserId,
            requestTenantId,
            state == null ? null : state.assetState(),
            state == null ? null : state.riskFlag(),
            state == null ? null : state.tenantId(),
            currentOwnerId,
            hasRetail,
            pendingExists
        );
    }

    public TransferAcceptContext resolveAcceptContext(TokenTransfer transfer) {
        TransferPassportState state = productReadPort.findPassportState(transfer.passportId())
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport not found"));

        boolean isC2C = transfer.transferType() == TransferType.C2C;
        String currentOwnerId = null;
        if (isC2C) {
            currentOwnerId = productReadPort.findCurrentOwnerId(transfer.passportId()).orElse(null);
        }

        return new TransferAcceptContext(
            isC2C,
            state.riskFlag(),
            currentOwnerId,
            transfer.fromOwnerId()
        );
    }

    public TransferAcceptContext resolveAcceptContext(String transferId) {
        TokenTransfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.TRANSFER_NOT_FOUND, "Transfer not found"));
        return resolveAcceptContext(transfer);
    }
}

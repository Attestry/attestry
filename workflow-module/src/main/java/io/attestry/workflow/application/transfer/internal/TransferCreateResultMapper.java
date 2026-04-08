package io.attestry.workflow.application.transfer.internal;

import io.attestry.workflow.application.transfer.result.CreateTransferResult;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import org.springframework.stereotype.Component;

@Component
public class TransferCreateResultMapper {

    public CreateTransferResult toResult(TokenTransfer transfer) {
        return new CreateTransferResult(
            transfer.transferId(),
            transfer.passportId(),
            transfer.transferType().name(),
            transfer.status().name(),
            transfer.acceptMethod().name(),
            transfer.qrNonce(),
            transfer.expiresAt()
        );
    }
}

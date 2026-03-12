package io.attestry.workflow.application.transfer;

import io.attestry.workflow.application.transfer.result.CancelTransferResult;
import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferCancelExecutor {

    private final TokenTransferRepository transferRepository;

    public CancelTransferResult cancel(TokenTransfer transfer, String cancelledByUserId, Instant now) {
        TokenTransfer cancelled = transfer.cancel(cancelledByUserId, now);
        TokenTransfer saved = transferRepository.save(cancelled);

        return new CancelTransferResult(
            saved.transferId(),
            saved.passportId(),
            saved.status().name(),
            saved.cancelledAt()
        );
    }
}

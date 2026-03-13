package io.attestry.workflow.domain.transfer.repository;

import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.model.TransferType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TokenTransferRepository {

    TokenTransfer save(TokenTransfer transfer);

    Optional<TokenTransfer> findById(String transferId);

    Optional<TokenTransfer> findLatestActivePendingByPassportId(String passportId, Instant now);

    Optional<TokenTransfer> findLatestActivePendingByPassportId(
        String passportId,
        Instant now,
        TransferType transferType,
        String tenantId
    );

    boolean existsActivePendingByPassportId(String passportId);

    List<TokenTransfer> findPendingExpiredBefore(Instant cutoff);
}

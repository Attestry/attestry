package io.attestry.workflow.domain.transfer.repository;

import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TokenTransferRepository {

    TokenTransfer save(TokenTransfer transfer);

    Optional<TokenTransfer> findById(String transferId);

    Optional<TokenTransfer> findLatestActivePendingByPassportId(String passportId, Instant now);

    boolean existsActivePendingByPassportId(String passportId);

    List<TokenTransfer> findPendingExpiredBefore(Instant cutoff);
}

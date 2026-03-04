package io.attestry.ledger.application.port;

import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import java.util.List;
import java.util.Optional;

public interface LedgerQueryRepositoryPort {
    List<LedgerEntry> findByPassportIdOrderBySeqAsc(String passportId);

    Optional<LedgerEntry> findByPassportIdAndLedgerId(String passportId, String ledgerId);

    List<String> findAllPassportIds();
}

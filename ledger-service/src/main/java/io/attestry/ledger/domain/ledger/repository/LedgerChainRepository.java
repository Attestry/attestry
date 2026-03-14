package io.attestry.ledger.domain.ledger.repository;

import io.attestry.ledger.domain.ledger.model.LedgerChain;
import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import java.util.Optional;

public interface LedgerChainRepository {

    LedgerChain loadForAppend(String passportId);

    AppendOutcome saveAppend(LedgerEntry newEntry, LedgerChain updatedChain);

    Optional<LedgerEntry> findEntryByIdempotencyKey(String idempotencyKey);

    record AppendOutcome(LedgerEntry entry, boolean duplicated) {}
}

package io.attestry.ledger.domain.ledger.service;

import io.attestry.ledger.domain.ledger.model.LedgerChainVerification;
import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import io.attestry.ledger.domain.ledger.model.PassportId;
import java.util.List;

public class LedgerChainVerifier {

    private final LedgerHashService hashService;

    public LedgerChainVerifier(LedgerHashService hashService) {
        this.hashService = hashService;
    }

    public LedgerChainVerification verify(PassportId passportId, List<LedgerEntry> entries) {
        if (entries.isEmpty()) {
            return new LedgerChainVerification(passportId, true, 0, null, null, null);
        }

        String expectedPrevHash = null;
        long expectedSeq = 1L;

        for (LedgerEntry entry : entries) {
            if (entry.seq() != expectedSeq) {
                return fail(passportId, entries.size(), entry.seq(), "seq discontinuity", lastEntryHash(entries));
            }
            if (entry.payloadCanonical() == null) {
                return fail(passportId, entries.size(), entry.seq(), "payload_canonical is missing", lastEntryHash(entries));
            }
            String recomputedDataHash = hashService.dataHash(entry.payloadCanonical());
            if (!recomputedDataHash.equals(entry.dataHash())) {
                return fail(passportId, entries.size(), entry.seq(), "data_hash mismatch", lastEntryHash(entries));
            }
            if (expectedPrevHash == null) {
                if (entry.prevHash() != null) {
                    return fail(passportId, entries.size(), entry.seq(), "genesis prev_hash must be null", lastEntryHash(entries));
                }
            } else if (!expectedPrevHash.equals(entry.prevHash())) {
                return fail(passportId, entries.size(), entry.seq(), "prev_hash mismatch", lastEntryHash(entries));
            }

            String recomputedEntryHash = hashService.entryHash(
                entry.prevHash(),
                entry.dataHash(),
                entry.seq(),
                entry.eventCategory(),
                entry.eventAction(),
                entry.actorRole(),
                entry.actorId(),
                entry.occurredAt()
            );
            if (!recomputedEntryHash.equals(entry.entryHash())) {
                return fail(passportId, entries.size(), entry.seq(), "entry_hash mismatch", lastEntryHash(entries));
            }

            expectedPrevHash = entry.entryHash();
            expectedSeq++;
        }

        return new LedgerChainVerification(
            passportId,
            true,
            entries.size(),
            null,
            null,
            lastEntryHash(entries)
        );
    }

    private LedgerChainVerification fail(
        PassportId passportId,
        long total,
        Long failedSeq,
        String reason,
        String latestEntryHash
    ) {
        return new LedgerChainVerification(passportId, false, total, failedSeq, reason, latestEntryHash);
    }

    private String lastEntryHash(List<LedgerEntry> entries) {
        return entries.isEmpty() ? null : entries.get(entries.size() - 1).entryHash();
    }
}

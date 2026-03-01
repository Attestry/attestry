package io.attestry.ledger.application.ledger;

import io.attestry.ledger.application.ledger.verification.LedgerVerificationResult;
import io.attestry.ledger.application.port.LedgerQueryRepositoryPort;
import io.attestry.ledger.application.usecase.LedgerVerificationUseCase;
import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import io.attestry.ledger.domain.ledger.service.LedgerHashService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerVerificationService implements LedgerVerificationUseCase {

    private final LedgerQueryRepositoryPort repository;
    private final LedgerHashService hashService;

    public LedgerVerificationService(LedgerQueryRepositoryPort repository, LedgerHashService hashService) {
        this.repository = repository;
        this.hashService = hashService;
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerVerificationResult verifyChain(String passportId) {
        requireText(passportId, "passportId");
        List<LedgerEntry> entries = repository.findByPassportIdOrderBySeqAsc(passportId);
        if (entries.isEmpty()) {
            return new LedgerVerificationResult(passportId, true, 0, null, null, null);
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

        return new LedgerVerificationResult(
            passportId,
            true,
            entries.size(),
            null,
            null,
            lastEntryHash(entries)
        );
    }

    private LedgerVerificationResult fail(
        String passportId,
        long total,
        Long failedSeq,
        String reason,
        String latestEntryHash
    ) {
        return new LedgerVerificationResult(passportId, false, total, failedSeq, reason, latestEntryHash);
    }

    private String lastEntryHash(List<LedgerEntry> entries) {
        return entries.isEmpty() ? null : entries.get(entries.size() - 1).entryHash();
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}

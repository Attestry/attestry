package io.attestry.ledger.application.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.ledger.application.ledger.verification.LedgerVerificationResult;
import io.attestry.ledger.application.port.LedgerQueryRepositoryPort;
import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import io.attestry.ledger.infrastructure.persistence.jpa.Sha256LedgerHashService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LedgerVerificationServiceTest {

    private final Sha256LedgerHashService hashService = new Sha256LedgerHashService();

    @Test
    void verifyChainReturnsValidForConsistentEntries() {
        String passportId = "p-1";
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-01T00:01:00Z");

        String p1 = "{\"a\":1}";
        String d1 = hashService.dataHash(p1);
        String e1 = hashService.entryHash(null, d1, 1L, "LIFECYCLE", "MINTED", "BRAND", "g1", t1);

        String p2 = "{\"a\":2}";
        String d2 = hashService.dataHash(p2);
        String e2 = hashService.entryHash(e1, d2, 2L, "LIFECYCLE", "TRANSFERRED", "RETAIL", "g2", t2);

        List<LedgerEntry> entries = List.of(
            new LedgerEntry("l1", passportId, 1L, "LIFECYCLE", "MINTED", "BRAND", "g1", t1, p1, p1, d1, null, e1, "k1", 1),
            new LedgerEntry("l2", passportId, 2L, "LIFECYCLE", "TRANSFERRED", "RETAIL", "g2", t2, p2, p2, d2, e1, e2, "k2", 1)
        );

        LedgerVerificationService service = new LedgerVerificationService(new InMemoryRepo(entries), hashService);
        LedgerVerificationResult result = service.verifyChain(passportId);

        assertTrue(result.valid());
        assertEquals(2L, result.totalEntries());
        assertEquals(e2, result.latestEntryHash());
    }

    @Test
    void verifyChainReturnsInvalidOnPrevHashMismatch() {
        String passportId = "p-2";
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-01T00:01:00Z");

        String p1 = "{\"a\":1}";
        String d1 = hashService.dataHash(p1);
        String e1 = hashService.entryHash(null, d1, 1L, "LIFECYCLE", "MINTED", "BRAND", "g1", t1);

        String p2 = "{\"a\":2}";
        String d2 = hashService.dataHash(p2);
        String e2 = hashService.entryHash("wrong-prev", d2, 2L, "LIFECYCLE", "TRANSFERRED", "RETAIL", "g2", t2);

        List<LedgerEntry> entries = List.of(
            new LedgerEntry("l1", passportId, 1L, "LIFECYCLE", "MINTED", "BRAND", "g1", t1, p1, p1, d1, null, e1, "k1", 1),
            new LedgerEntry("l2", passportId, 2L, "LIFECYCLE", "TRANSFERRED", "RETAIL", "g2", t2, p2, p2, d2, "wrong-prev", e2, "k2", 1)
        );

        LedgerVerificationService service = new LedgerVerificationService(new InMemoryRepo(entries), hashService);
        LedgerVerificationResult result = service.verifyChain(passportId);

        assertFalse(result.valid());
        assertEquals(2L, result.failedSeq());
        assertEquals("prev_hash mismatch", result.reason());
    }

    private static final class InMemoryRepo implements LedgerQueryRepositoryPort {
        private final List<LedgerEntry> entries;

        private InMemoryRepo(List<LedgerEntry> entries) {
            this.entries = new ArrayList<>(entries);
        }

        @Override
        public List<LedgerEntry> findByPassportIdOrderBySeqAsc(String passportId) {
            return entries.stream().filter(e -> e.passportId().equals(passportId)).toList();
        }

        @Override
        public Optional<LedgerEntry> findByPassportIdAndLedgerId(String passportId, String ledgerId) {
            return entries.stream()
                .filter(e -> e.passportId().equals(passportId) && e.ledgerId().equals(ledgerId))
                .findFirst();
        }
    }
}

package io.attestry.ledger.domain.ledger.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.ledger.domain.ledger.model.LedgerChainVerification;
import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import io.attestry.ledger.domain.ledger.model.PassportId;
import io.attestry.ledger.infrastructure.persistence.jpa.support.Sha256LedgerHashService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LedgerChainVerifierTest {

    private final Sha256LedgerHashService hashService = new Sha256LedgerHashService();
    private final LedgerChainVerifier verifier = new LedgerChainVerifier(hashService);

    private static final PassportId PASSPORT = PassportId.of("passport-1");
    private static final Instant T1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-01-01T00:01:00Z");
    private static final Instant T3 = Instant.parse("2026-01-01T00:02:00Z");

    @Test
    void emptyChain_returnsValid() {
        LedgerChainVerification result = verifier.verify(PASSPORT, List.of());

        assertTrue(result.valid());
        assertEquals(0, result.totalEntries());
        assertNull(result.failedSeq());
        assertNull(result.reason());
    }

    @Test
    void singleGenesisEntry_valid() {
        LedgerEntry entry = buildEntry(1L, null, T1, "{\"a\":1}");

        LedgerChainVerification result = verifier.verify(PASSPORT, List.of(entry));

        assertTrue(result.valid());
        assertEquals(1, result.totalEntries());
        assertEquals(entry.entryHash(), result.latestEntryHash());
    }

    @Test
    void twoChainedEntries_valid() {
        LedgerEntry e1 = buildEntry(1L, null, T1, "{\"a\":1}");
        LedgerEntry e2 = buildEntry(2L, e1.entryHash(), T2, "{\"a\":2}");

        LedgerChainVerification result = verifier.verify(PASSPORT, List.of(e1, e2));

        assertTrue(result.valid());
        assertEquals(2, result.totalEntries());
        assertEquals(e2.entryHash(), result.latestEntryHash());
    }

    @Test
    void seqDiscontinuity_returnsInvalid() {
        LedgerEntry e1 = buildEntry(1L, null, T1, "{\"a\":1}");
        LedgerEntry e3 = buildEntry(3L, e1.entryHash(), T2, "{\"a\":3}");

        LedgerChainVerification result = verifier.verify(PASSPORT, List.of(e1, e3));

        assertFalse(result.valid());
        assertEquals(3L, result.failedSeq());
        assertEquals("seq discontinuity", result.reason());
    }

    @Test
    void dataHashMismatch_returnsInvalid() {
        String p1 = "{\"a\":1}";
        String d1 = hashService.dataHash(p1);
        String e1Hash = hashService.entryHash(null, d1, 1L, "LIFECYCLE", "MINTED", "BRAND", "g1", T1);

        // entry with tampered dataHash
        LedgerEntry entry = LedgerEntry.rehydrate(
            "l1", PASSPORT.value(), 1L, "LIFECYCLE", "MINTED", "BRAND", "g1", T1,
            p1, p1, "tampered_data_hash_0000000000000000000000000000000000", null, e1Hash, null, 1
        );

        LedgerChainVerification result = verifier.verify(PASSPORT, List.of(entry));

        assertFalse(result.valid());
        assertEquals(1L, result.failedSeq());
        assertEquals("data_hash mismatch", result.reason());
    }

    @Test
    void genesisPrevHashNotNull_returnsInvalid() {
        String p1 = "{\"a\":1}";
        String d1 = hashService.dataHash(p1);
        // compute entryHash with a non-null prevHash
        String fakeEntryHash = hashService.entryHash("not-null", d1, 1L, "LIFECYCLE", "MINTED", "BRAND", "g1", T1);

        LedgerEntry entry = LedgerEntry.rehydrate(
            "l1", PASSPORT.value(), 1L, "LIFECYCLE", "MINTED", "BRAND", "g1", T1,
            p1, p1, d1, "not-null", fakeEntryHash, null, 1
        );

        LedgerChainVerification result = verifier.verify(PASSPORT, List.of(entry));

        assertFalse(result.valid());
        assertEquals(1L, result.failedSeq());
        assertEquals("genesis prev_hash must be null", result.reason());
    }

    @Test
    void prevHashMismatch_returnsInvalid() {
        LedgerEntry e1 = buildEntry(1L, null, T1, "{\"a\":1}");

        // second entry with wrong prevHash
        String p2 = "{\"a\":2}";
        String d2 = hashService.dataHash(p2);
        String wrongPrev = "wrong_prev_hash_000000000000000000000000000000000000";
        String e2Hash = hashService.entryHash(wrongPrev, d2, 2L, "LIFECYCLE", "TRANSFERRED", "RETAIL", "g2", T2);

        LedgerEntry e2 = LedgerEntry.rehydrate(
            "l2", PASSPORT.value(), 2L, "LIFECYCLE", "TRANSFERRED", "RETAIL", "g2", T2,
            p2, p2, d2, wrongPrev, e2Hash, null, 1
        );

        LedgerChainVerification result = verifier.verify(PASSPORT, List.of(e1, e2));

        assertFalse(result.valid());
        assertEquals(2L, result.failedSeq());
        assertEquals("prev_hash mismatch", result.reason());
    }

    @Test
    void entryHashMismatch_returnsInvalid() {
        String p1 = "{\"a\":1}";
        String d1 = hashService.dataHash(p1);

        LedgerEntry entry = LedgerEntry.rehydrate(
            "l1", PASSPORT.value(), 1L, "LIFECYCLE", "MINTED", "BRAND", "g1", T1,
            p1, p1, d1, null, "tampered_entry_hash_00000000000000000000000000000000", null, 1
        );

        LedgerChainVerification result = verifier.verify(PASSPORT, List.of(entry));

        assertFalse(result.valid());
        assertEquals(1L, result.failedSeq());
        assertEquals("entry_hash mismatch", result.reason());
    }

    private LedgerEntry buildEntry(long seq, String prevHash, Instant occurredAt, String payload) {
        String dataHash = hashService.dataHash(payload);
        String entryHash = hashService.entryHash(
            prevHash, dataHash, seq, "LIFECYCLE", "MINTED", "BRAND", "g1", occurredAt
        );
        return LedgerEntry.rehydrate(
            "l-" + seq, PASSPORT.value(), seq, "LIFECYCLE", "MINTED", "BRAND", "g1", occurredAt,
            payload, payload, dataHash, prevHash, entryHash, null, 1
        );
    }
}

package io.attestry.ledger.domain.ledger.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.ledger.domain.LedgerDomainException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LedgerEntryTest {

    private static final String PASSPORT_ID = "passport-1";
    private static final Instant T1 = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void append_createsValidEntry() {
        PassportId passportId = PassportId.of(PASSPORT_ID);
        LedgerChainState.PlannedAppend planned = new LedgerChainState.PlannedAppend(
            1L, null, "entry-hash-1",
            new LedgerChainState(PASSPORT_ID, 1L, "entry-hash-1")
        );

        LedgerEntry entry = LedgerEntry.append(
            passportId, planned,
            "LIFECYCLE", "MINTED", "BRAND", "actor-1",
            T1, "{\"a\":1}", "{\"a\":1}", "data-hash-1", "idem-key-1", 1
        );

        assertNotNull(entry.ledgerId());
        assertEquals(PASSPORT_ID, entry.passportId());
        assertEquals(1L, entry.seq());
        assertEquals("LIFECYCLE", entry.eventCategory());
        assertEquals("MINTED", entry.eventAction());
        assertEquals("BRAND", entry.actorRole());
        assertEquals("actor-1", entry.actorId());
        assertEquals(T1, entry.occurredAt());
        assertEquals("{\"a\":1}", entry.payloadJson());
        assertEquals("{\"a\":1}", entry.payloadCanonical());
        assertEquals("data-hash-1", entry.dataHash());
        assertNull(entry.prevHash());
        assertEquals("entry-hash-1", entry.entryHash());
        assertEquals("idem-key-1", entry.idempotencyKey());
        assertEquals(1, entry.schemaVersion());
    }

    @Test
    void rehydrate_restoresEntry() {
        LedgerEntry entry = LedgerEntry.rehydrate(
            "ledger-id-1", PASSPORT_ID, 2L,
            "LIFECYCLE", "TRANSFERRED", "RETAIL", "actor-2",
            T1, "{\"b\":2}", "{\"b\":2}", "data-hash-2",
            "prev-hash-1", "entry-hash-2", "idem-key-2", 1
        );

        assertEquals("ledger-id-1", entry.ledgerId());
        assertEquals(PASSPORT_ID, entry.passportId());
        assertEquals(2L, entry.seq());
        assertEquals("LIFECYCLE", entry.eventCategory());
        assertEquals("TRANSFERRED", entry.eventAction());
        assertEquals("RETAIL", entry.actorRole());
        assertEquals("actor-2", entry.actorId());
        assertEquals(T1, entry.occurredAt());
        assertEquals("{\"b\":2}", entry.payloadJson());
        assertEquals("{\"b\":2}", entry.payloadCanonical());
        assertEquals("data-hash-2", entry.dataHash());
        assertEquals("prev-hash-1", entry.prevHash());
        assertEquals("entry-hash-2", entry.entryHash());
        assertEquals("idem-key-2", entry.idempotencyKey());
        assertEquals(1, entry.schemaVersion());
    }

    @Test
    void validation_failsWhenPassportIdBlank() {
        assertThrows(LedgerDomainException.class, () ->
            new LedgerEntry(
                "ledger-id-1", "  ", 1L,
                "LIFECYCLE", "MINTED", "BRAND", "actor-1",
                T1, "{\"a\":1}", "{\"a\":1}", "data-hash-1",
                null, "entry-hash-1", null, 1
            )
        );
    }

    @Test
    void validation_failsWhenSeqZero() {
        LedgerDomainException ex = assertThrows(LedgerDomainException.class, () ->
            new LedgerEntry(
                "ledger-id-1", PASSPORT_ID, 0L,
                "LIFECYCLE", "MINTED", "BRAND", "actor-1",
                T1, "{\"a\":1}", "{\"a\":1}", "data-hash-1",
                null, "entry-hash-1", null, 1
            )
        );

        assertEquals("seq must be > 0", ex.getMessage());
    }

    @Test
    void validation_normalizesBlankIdempotencyKey() {
        LedgerEntry entry = new LedgerEntry(
            "ledger-id-1", PASSPORT_ID, 1L,
            "LIFECYCLE", "MINTED", "BRAND", "actor-1",
            T1, "{\"a\":1}", "{\"a\":1}", "data-hash-1",
            null, "entry-hash-1", "   ", 1
        );

        assertNull(entry.idempotencyKey());
    }
}

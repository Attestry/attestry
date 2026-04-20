package io.attestry.ledger.domain.ledger.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.attestry.ledger.domain.ledger.service.LedgerHashService;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LedgerChainStateTest {

    private static final String PASSPORT_ID = "passport-1";
    private static final Instant T1 = Instant.parse("2026-01-01T00:00:00Z");

    private final LedgerHashService hashService = new StubLedgerHashService();

    @Test
    void initialize_setsDefaults() {
        LedgerChainState state = LedgerChainState.initialize(PASSPORT_ID);

        assertEquals(PASSPORT_ID, state.passportId());
        assertEquals(0L, state.lastSeq());
        assertNull(state.lastHash());
    }

    @Test
    void planNext_incrementsSeq() {
        LedgerChainState state = LedgerChainState.initialize(PASSPORT_ID);

        LedgerChainState.PlannedAppend planned = state.planNext(
            hashService, "d1", "LIFECYCLE", "MINTED", "BRAND", "actor-1", T1
        );

        assertEquals(1L, planned.seq());
        assertEquals(1L, planned.nextState().lastSeq());
    }

    @Test
    void planNext_setsPrevHash() {
        LedgerChainState state = LedgerChainState.of(PASSPORT_ID, 1L, "abc");

        LedgerChainState.PlannedAppend planned = state.planNext(
            hashService, "d2", "LIFECYCLE", "TRANSFERRED", "RETAIL", "actor-2", T1
        );

        assertEquals("abc", planned.prevHash());
    }

    @Test
    void planNext_computesEntryHash() {
        LedgerChainState state = LedgerChainState.initialize(PASSPORT_ID);

        LedgerChainState.PlannedAppend planned = state.planNext(
            hashService, "d1", "LIFECYCLE", "MINTED", "BRAND", "actor-1", T1
        );

        String expectedHash = hashService.entryHash(
            null, "d1", 1L, "LIFECYCLE", "MINTED", "BRAND", "actor-1", T1
        );
        assertEquals(expectedHash, planned.entryHash());
        assertEquals(expectedHash, planned.nextState().lastHash());
    }

    @Test
    void of_normalizesNullSeq() {
        LedgerChainState state = LedgerChainState.of(PASSPORT_ID, null, null);

        assertEquals(PASSPORT_ID, state.passportId());
        assertEquals(0L, state.lastSeq());
        assertNull(state.lastHash());
    }

    /**
     * Stub hash service that returns predictable, deterministic values.
     */
    private static final class StubLedgerHashService implements LedgerHashService {

        @Override
        public String dataHash(String payloadCanonical) {
            return "stub-data-" + payloadCanonical;
        }

        @Override
        public String entryHash(
            String prevHash, String dataHash, long seq,
            String eventCategory, String eventAction,
            String actorRole, String actorId, Instant occurredAt
        ) {
            return "stub-entry-" + seq + "-" + (prevHash == null ? "null" : prevHash) + "-" + dataHash;
        }
    }
}

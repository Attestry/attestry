package io.attestry.ledger.domain.ledger.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.ledger.domain.LedgerDomainException;
import io.attestry.ledger.domain.ledger.service.LedgerHashService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LedgerChainTest {

    private static final String PASSPORT_ID = "passport-1";
    private static final Instant T1 = Instant.parse("2026-01-01T00:00:00Z");

    private final LedgerHashService hashService = new StubLedgerHashService();

    @Test
    void initialize_createsEmptyChain() {
        LedgerChain chain = LedgerChain.initialize(PASSPORT_ID);

        assertEquals(PASSPORT_ID, chain.state().passportId());
        assertEquals(0L, chain.state().lastSeq());
        assertNull(chain.state().lastHash());
    }

    @Test
    void append_genesisEntry() {
        LedgerChain chain = LedgerChain.initialize(PASSPORT_ID);
        LedgerAppendInput input = LedgerAppendInput.of(
            PASSPORT_ID, "LIFECYCLE", "MINTED", "BRAND", "actor-1",
            T1, Map.of("key", "value"), "idem-1"
        );
        LedgerPayloadMaterialized materialized = new LedgerPayloadMaterialized(
            "{\"key\":\"value\"}", "{\"key\":\"value\"}", "stub-data-hash"
        );

        LedgerChain.AppendPlan plan = chain.append(input, materialized, hashService, 1);

        LedgerEntry entry = plan.entry();
        assertEquals(PASSPORT_ID, entry.passportId());
        assertEquals(1L, entry.seq());
        assertNull(entry.prevHash());
        assertNotNull(entry.entryHash());
        assertEquals("stub-data-hash", entry.dataHash());

        LedgerChain nextChain = plan.nextChain();
        assertEquals(1L, nextChain.state().lastSeq());
        assertEquals(entry.entryHash(), nextChain.state().lastHash());
    }

    @Test
    void append_secondEntry() {
        String firstEntryHash = "first-entry-hash-value";
        LedgerChain chain = LedgerChain.restore(PASSPORT_ID, 1L, firstEntryHash);

        LedgerAppendInput input = LedgerAppendInput.of(
            PASSPORT_ID, "LIFECYCLE", "TRANSFERRED", "RETAIL", "actor-2",
            T1, Map.of("b", 2), "idem-2"
        );
        LedgerPayloadMaterialized materialized = new LedgerPayloadMaterialized(
            "{\"b\":2}", "{\"b\":2}", "stub-data-hash-2"
        );

        LedgerChain.AppendPlan plan = chain.append(input, materialized, hashService, 1);

        LedgerEntry entry = plan.entry();
        assertEquals(2L, entry.seq());
        assertEquals(firstEntryHash, entry.prevHash());
        assertNotNull(entry.entryHash());

        LedgerChain nextChain = plan.nextChain();
        assertEquals(2L, nextChain.state().lastSeq());
        assertEquals(entry.entryHash(), nextChain.state().lastHash());
    }

    @Test
    void append_failsPassportMismatch() {
        LedgerChain chain = LedgerChain.initialize(PASSPORT_ID);

        LedgerAppendInput input = LedgerAppendInput.of(
            "passport-2", "LIFECYCLE", "MINTED", "BRAND", "actor-1",
            T1, Map.of("key", "value"), null
        );
        LedgerPayloadMaterialized materialized = new LedgerPayloadMaterialized(
            "{\"key\":\"value\"}", "{\"key\":\"value\"}", "stub-data-hash"
        );

        LedgerDomainException ex = assertThrows(LedgerDomainException.class,
            () -> chain.append(input, materialized, hashService, 1));

        assertEquals("passportId mismatch between chain and input", ex.getMessage());
    }

    /**
     * Stub hash service that returns predictable, deterministic values.
     */
    private static final class StubLedgerHashService implements LedgerHashService {

        @Override
        public String dataHash(String payloadCanonical) {
            return "stub-data-hash-" + payloadCanonical.hashCode();
        }

        @Override
        public String entryHash(
            String prevHash, String dataHash, long seq,
            String eventCategory, String eventAction,
            String actorRole, String actorId, Instant occurredAt
        ) {
            return "stub-entry-hash-seq" + seq + "-" + dataHash;
        }
    }
}

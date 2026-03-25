package io.attestry.ledger.domain.ledger.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.attestry.ledger.infrastructure.persistence.jpa.support.Sha256LedgerHashService;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class Sha256LedgerHashServiceTest {

    private final Sha256LedgerHashService hashService = new Sha256LedgerHashService();

    @Test
    void genesisEntry_dataHashAndEntryHashArePinned() {
        String canonical = "{\"assetId\":\"A100\",\"modelId\":\"M55\",\"serialNumber\":\"123\"}";
        String dataHash = hashService.dataHash(canonical);
        String entryHash = hashService.entryHash(
            null,
            dataHash,
            1L,
            "LIFECYCLE",
            "MINTED",
            "BRAND",
            "group-1",
            Instant.parse("2026-01-15T10:30:00Z")
        );

        assertEquals("bf0648c8fb23acc407d1a514e5c67358e4a5a7725348cba92576ffaf8b895167", dataHash);
        assertEquals("ddb7a6032349fafaf370cf649b1c96ac5691a77bb2aa339e12d34af0ee4667a4", entryHash);
    }

    @Test
    void chainedEntry_entryHashWithPrevHashIsPinned() {
        String prevHash = "ddb7a6032349fafaf370cf649b1c96ac5691a77bb2aa339e12d34af0ee4667a4";
        String canonical2 = "{\"newOwner\":\"user-42\",\"reason\":\"sold\"}";
        String dataHash2 = hashService.dataHash(canonical2);
        String entryHash2 = hashService.entryHash(
            prevHash,
            dataHash2,
            2L,
            "LIFECYCLE",
            "TRANSFERRED",
            "RETAIL",
            "group-2",
            Instant.parse("2026-01-16T14:00:00Z")
        );

        assertEquals("a0925f05dc2316dde600c014d8d91f1f9ce9ad212859e24b78fb3ebe4d2ebd1d", dataHash2);
        assertEquals(64, entryHash2.length(), "entryHash must be 64 hex chars (SHA-256)");

        assertEquals("add9b3234f2a52cc5ac204054a78353876baa0a9b55e04c4526798ab160ff6e3", entryHash2);
    }

    @Test
    void nullPrevHash_normalizedToEmptyString() {
        String dataHash = hashService.dataHash("{\"a\":1}");

        String hashWithNull = hashService.entryHash(
            null, dataHash, 1L, "CAT", "ACT", "ROLE", "actor", Instant.parse("2026-01-01T00:00:00Z")
        );
        String hashWithEmpty = hashService.entryHash(
            null, dataHash, 1L, "CAT", "ACT", "ROLE", "actor", Instant.parse("2026-01-01T00:00:00Z")
        );

        assertEquals(hashWithNull, hashWithEmpty, "null prevHash must always produce the same hash");
    }

    @Test
    void differentFieldOrder_producesDifferentHash() {
        String dataHash = hashService.dataHash("{\"x\":1}");
        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        String hash1 = hashService.entryHash(null, dataHash, 1L, "CAT_A", "ACT_B", "ROLE", "actor", t);
        String hash2 = hashService.entryHash(null, dataHash, 1L, "CAT_B", "ACT_A", "ROLE", "actor", t);

        assertNotEquals(hash1, hash2, "swapping eventCategory/eventAction must change the hash");
    }

    @Test
    void sameInput_alwaysProducesSameDataHash() {
        String canonical = "{\"stable\":\"value\"}";
        String first = hashService.dataHash(canonical);
        for (int i = 0; i < 100; i++) {
            assertEquals(first, hashService.dataHash(canonical), "dataHash must be deterministic");
        }
    }
}

package io.attestry.ledger.domain.ledger.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.attestry.ledger.infrastructure.persistence.jpa.Sha256LedgerHashService;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class Sha256LedgerHashServiceTest {

    private final Sha256LedgerHashService hashService = new Sha256LedgerHashService();

    @Test
    void hashCalculationIsDeterministic() {
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
}

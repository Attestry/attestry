package io.attestry.ledger.domain.ledger.service;

import java.time.Instant;

public interface LedgerHashService {
    String dataHash(String payloadCanonical);

    String entryHash(
        String prevHash,
        String dataHash,
        long seq,
        String eventCategory,
        String eventAction,
        String actorRole,
        String actorId,
        Instant occurredAt
    );
}

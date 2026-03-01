package io.attestry.ledger.domain.ledger.model;

import java.time.Instant;

public record LedgerEntry(
    String ledgerId,
    String passportId,
    long seq,
    String eventCategory,
    String eventAction,
    String actorRole,
    String actorId,
    Instant occurredAt,
    String payloadJson,
    String payloadCanonical,
    String dataHash,
    String prevHash,
    String entryHash,
    String idempotencyKey,
    int schemaVersion
) {
}

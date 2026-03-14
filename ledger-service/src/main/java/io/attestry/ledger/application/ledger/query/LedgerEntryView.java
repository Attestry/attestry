package io.attestry.ledger.application.ledger.query;

import java.time.Instant;
import java.util.Map;

public record LedgerEntryView(
    String ledgerId,
    String passportId,
    long seq,
    String eventCategory,
    String eventAction,
    String actorRole,
    String actorId,
    Instant occurredAt,
    Map<String, Object> payload,
    String dataHash,
    String prevHash,
    String entryHash
) {
}

package io.attestry.ledger.application.ledger.command;

import java.time.Instant;
import java.util.Map;

public record AppendLedgerEntryCommand(
    String passportId,
    String eventCategory,
    String eventAction,
    String actorRole,
    String actorId,
    Instant occurredAt,
    Map<String, Object> payload,
    String idempotencyKey
) {
}

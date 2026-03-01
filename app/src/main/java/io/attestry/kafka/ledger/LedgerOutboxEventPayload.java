package io.attestry.kafka.ledger;

import java.time.Instant;
import java.util.Map;

public record LedgerOutboxEventPayload(
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

package io.attestry.commonlib.outbox;

import java.time.Instant;
import java.util.Map;

public record OutboxEventEnvelope(
    String aggregateType,
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

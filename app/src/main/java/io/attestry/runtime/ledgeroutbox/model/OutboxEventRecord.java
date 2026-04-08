package io.attestry.runtime.ledgeroutbox.model;

import java.time.Instant;

public record OutboxEventRecord(
    String eventId,
    OutboxAggregateType aggregateType,
    String aggregateId,
    OutboxEventType eventType,
    String payload,
    int retryCount,
    Instant createdAt
) {
}

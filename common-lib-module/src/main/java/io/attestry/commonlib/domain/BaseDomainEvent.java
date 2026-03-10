package io.attestry.commonlib.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class BaseDomainEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredAt;
    private final String aggregateId;

    protected BaseDomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.aggregateId = aggregateId;
    }

    protected BaseDomainEvent(String eventId, String aggregateId, LocalDateTime occurredAt) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.occurredAt = occurredAt;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getAggregateId() {
        return aggregateId;
    }

    @Override
    public String toString() {
        return String.format("%s[eventId=%s, aggregateId=%s, occurredAt=%s]",
                getEventType(), eventId, aggregateId, occurredAt);
    }
}

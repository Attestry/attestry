package io.attestry.commonlib.domain;

import java.time.LocalDateTime;

public interface DomainEvent {

    String getEventId();

    LocalDateTime getOccurredAt();

    default String getEventType() {
        return this.getClass().getSimpleName();
    }

    String getAggregateId();
}

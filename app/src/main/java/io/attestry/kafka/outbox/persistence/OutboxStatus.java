package io.attestry.kafka.outbox.persistence;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}

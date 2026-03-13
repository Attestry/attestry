package io.attestry.ledgerservice.outbox.persistence;

public enum LedgerOutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}

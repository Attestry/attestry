package io.attestry.runtime.ledgeroutbox.model;

public record PublishAttempt(OutboxEventRecord event, Throwable error) {
}

package io.attestry.job.outbox.model;

public record PublishAttempt(OutboxEventRecord event, Throwable error) {
}

package io.attestry.product.application.command.internal;

import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.product.domain.passport.model.ProductPassport;

public record MintExecution(ProductPassport passport, OutboxEventEnvelope event, String outboxEventId) {
}

package io.attestry.product.application.command.dto;

import io.attestry.product.domain.event.LedgerEventEnvelope;
import io.attestry.product.domain.passport.model.ProductPassport;

public record MintExecution(ProductPassport passport, LedgerEventEnvelope event, String outboxEventId) {
}

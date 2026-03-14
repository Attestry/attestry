package io.attestry.product.application.command.dto;

import io.attestry.product.domain.passport.model.ProductPassport;

public record RiskExecution(ProductPassport passport, String outboxEventId) {
}

package io.attestry.product.application.command.support;

import io.attestry.product.domain.passport.model.ProductPassport;

public record VoidExecution(ProductPassport passport, String outboxEventId) {
}

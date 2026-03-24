package io.attestry.product.application.command.model;

public record VoidCommand(String tenantId, String passportId, String reason, String note) {
}

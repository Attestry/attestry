package io.attestry.product.application.dto.command;

public record VoidCommand(String tenantId, String passportId, String reason, String note) {
}

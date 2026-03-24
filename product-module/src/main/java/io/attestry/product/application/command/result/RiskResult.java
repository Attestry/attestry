package io.attestry.product.application.command.result;

public record RiskResult(String assetId, String riskFlag, String outboxEventId) {
}

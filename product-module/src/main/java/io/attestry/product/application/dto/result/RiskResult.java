package io.attestry.product.application.dto.result;

public record RiskResult(String assetId, String riskFlag, String outboxEventId) {
}

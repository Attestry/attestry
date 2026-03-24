package io.attestry.product.interfaces.http.command.dto.response;

import io.attestry.product.application.command.result.RiskResult;

public record RiskResponse(
    String assetId,
    String riskFlag,
    String outboxEventId
) {
    public static RiskResponse from(RiskResult result) {
        return new RiskResponse(result.assetId(), result.riskFlag(), result.outboxEventId());
    }
}

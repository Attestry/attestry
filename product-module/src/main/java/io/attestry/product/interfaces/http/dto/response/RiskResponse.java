package io.attestry.product.interfaces.http.dto.response;

import io.attestry.product.application.dto.result.RiskResult;

public record RiskResponse(
    String assetId,
    String riskFlag,
    String outboxEventId
) {
    public static RiskResponse from(RiskResult result) {
        return new RiskResponse(result.assetId(), result.riskFlag(), result.outboxEventId());
    }
}

package io.attestry.product.interfaces.http.dto.response;

import io.attestry.product.application.dto.result.MyPassportResult;
import java.time.Instant;

public record MyPassportResponse(
    String passportId,
    String qrPublicCode,
    String tenantId,
    String assetId,
    String serialNumber,
    String modelName,
    String assetState,
    String riskFlag,
    Instant ownedSince
) {
    public static MyPassportResponse from(MyPassportResult result) {
        return new MyPassportResponse(
            result.passportId(), result.qrPublicCode(), result.tenantId(),
            result.assetId(), result.serialNumber(), result.modelName(),
            result.assetState(), result.riskFlag(), result.ownedSince()
        );
    }
}

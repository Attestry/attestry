package io.attestry.product.interfaces.http.dto.response;

import io.attestry.product.application.dto.result.DistributedPassportDetailResult;
import java.time.Instant;

public record DistributedPassportDetailResponse(
    String passportId,
    String qrPublicCode,
    String serialNumber,
    String modelId,
    String modelName,
    String assetState,
    String riskFlag,
    Instant manufacturedAt,
    String productionBatch,
    String factoryCode
) {
    public static DistributedPassportDetailResponse from(DistributedPassportDetailResult result) {
        return new DistributedPassportDetailResponse(
            result.passportId(), result.qrPublicCode(), result.serialNumber(),
            result.modelId(), result.modelName(), result.assetState(), result.riskFlag(),
            result.manufacturedAt(), result.productionBatch(), result.factoryCode()
        );
    }
}

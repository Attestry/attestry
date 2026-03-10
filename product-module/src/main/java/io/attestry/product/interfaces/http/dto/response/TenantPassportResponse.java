package io.attestry.product.interfaces.http.dto.response;

import io.attestry.product.application.dto.result.TenantPassportResult;
import java.time.Instant;

public record TenantPassportResponse(
    String passportId,
    String serialNumber,
    String modelId,
    String modelName,
    String assetState,
    Instant createdAt
) {
    public static TenantPassportResponse from(TenantPassportResult result) {
        return new TenantPassportResponse(
            result.passportId(), result.serialNumber(), result.modelId(),
            result.modelName(), result.assetState(), result.createdAt()
        );
    }
}

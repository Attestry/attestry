package io.attestry.product.interfaces.http.query.dto.response;

import io.attestry.product.application.dto.view.TenantPassportView;
import java.time.Instant;

public record TenantPassportResponse(
    String passportId,
    String serialNumber,
    String modelId,
    String modelName,
    String assetState,
    Instant createdAt
) {
    public static TenantPassportResponse from(TenantPassportView result) {
        return new TenantPassportResponse(
            result.passportId(), result.serialNumber(), result.modelId(),
            result.modelName(), result.assetState(), result.createdAt()
        );
    }
}

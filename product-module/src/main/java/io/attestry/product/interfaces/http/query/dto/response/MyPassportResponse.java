package io.attestry.product.interfaces.http.query.dto.response;

import io.attestry.product.application.dto.view.MyPassportView;
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
    public static MyPassportResponse from(MyPassportView view) {
        return new MyPassportResponse(
            view.passportId(), view.qrPublicCode(), view.tenantId(),
            view.assetId(), view.serialNumber(), view.modelName(),
            view.assetState(), view.riskFlag(), view.ownedSince()
        );
    }
}

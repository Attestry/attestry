package io.attestry.product.application.port;

import java.time.Instant;
import java.util.List;

public interface MyPassportQueryPort {

    List<MyPassportView> findByOwnerId(String ownerId);

    record MyPassportView(
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
    }
}

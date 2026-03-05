package io.attestry.product.application.port;

import java.time.Instant;
import java.util.List;

public interface GroupPassportQueryPort {

    List<GroupPassportView> findByTenantAndGroup(String tenantId, String groupId);

    record GroupPassportView(
        String passportId,
        String qrPublicCode,
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        Instant manufacturedAt,
        String assetState,
        String riskFlag,
        String ownerId,
        Instant createdAt
    ) {
    }
}

package io.attestry.product.application.port;

import java.time.Instant;
import java.util.List;

public interface GroupPassportQueryPort {

    PagedResult findByTenant(String tenantId, int page, int size);

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

    record PagedResult(List<GroupPassportView> content, int page, int size, long totalElements, int totalPages) {
    }
}

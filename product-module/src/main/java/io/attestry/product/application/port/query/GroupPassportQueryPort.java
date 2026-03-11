package io.attestry.product.application.port.query;

import java.time.Instant;
import java.util.List;

public interface GroupPassportQueryPort {

    PagedResult findByTenant(
        String tenantId,
        int page,
        int size,
        String assetState,
        Instant createdFrom,
        Instant createdTo,
        String keyword
    );

    record GroupPassportRow(
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

    record PagedResult(List<GroupPassportRow> content, int page, int size, long totalElements, int totalPages) {
    }
}

package io.attestry.workflow.application.port;

import java.time.Instant;
import java.util.List;

public interface CompletedTransferQueryPort {

    PagedResult findCompletedB2CByTenantId(String tenantId, String sourceTenantId, int page, int size);

    boolean existsCompletedB2CByTenantAndPassportId(String tenantId, String passportId);

    record CompletedTransferRow(
        String transferId,
        String passportId,
        String sourceTenantId,
        String serialNumber,
        String modelName,
        String assetState,
        String toOwnerId,
        String acceptMethod,
        Instant completedAt
    ) {
    }

    record PagedResult(List<CompletedTransferRow> content, int page, int size, long totalElements, int totalPages) {
    }
}

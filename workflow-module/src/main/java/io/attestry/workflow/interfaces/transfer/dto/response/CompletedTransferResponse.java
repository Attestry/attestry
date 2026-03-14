package io.attestry.workflow.interfaces.transfer.dto.response;

import java.time.Instant;

public record CompletedTransferResponse(
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

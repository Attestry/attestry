package io.attestry.workflow.application.transfer.view;

import java.time.Instant;

public record CompletedTransferView(
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

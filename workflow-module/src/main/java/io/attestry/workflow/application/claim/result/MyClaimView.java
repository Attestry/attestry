package io.attestry.workflow.application.claim.result;

import java.time.Instant;

public record MyClaimView(
    String claimId,
    String tenantId,
    String serialNumber,
    String modelName,
    String status,
    Instant submittedAt,
    String rejectionReason,
    String passportId,
    String assetId
) {
}

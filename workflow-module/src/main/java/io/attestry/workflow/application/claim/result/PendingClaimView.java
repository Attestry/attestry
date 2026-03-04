package io.attestry.workflow.application.claim.result;

import java.time.Instant;

public record PendingClaimView(
    String claimId,
    String claimantUserId,
    String serialNumber,
    String modelName,
    String evidenceGroupId,
    String note,
    String status,
    Instant submittedAt
) {
}

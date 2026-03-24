package io.attestry.workflow.application.claim.view;

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

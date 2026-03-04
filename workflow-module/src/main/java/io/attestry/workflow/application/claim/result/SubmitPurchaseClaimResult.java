package io.attestry.workflow.application.claim.result;

import java.time.Instant;

public record SubmitPurchaseClaimResult(
    String claimId,
    String status,
    Instant submittedAt
) {
}

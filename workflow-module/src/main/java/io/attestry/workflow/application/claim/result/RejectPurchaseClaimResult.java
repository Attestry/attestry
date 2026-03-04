package io.attestry.workflow.application.claim.result;

public record RejectPurchaseClaimResult(
    String claimId,
    String status,
    String rejectionReason
) {
}

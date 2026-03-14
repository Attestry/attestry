package io.attestry.workflow.application.claim.result;

public record ApprovePurchaseClaimResult(
    String claimId,
    String passportId,
    String assetId,
    String qrPublicCode,
    String status
) {
}

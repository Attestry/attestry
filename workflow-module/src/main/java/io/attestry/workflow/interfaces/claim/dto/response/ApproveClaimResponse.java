package io.attestry.workflow.interfaces.claim.dto.response;

import io.attestry.workflow.application.claim.result.ApprovePurchaseClaimResult;

public record ApproveClaimResponse(
    String claimId, String passportId, String assetId, String qrPublicCode, String status
) {
    public static ApproveClaimResponse from(ApprovePurchaseClaimResult result) {
        return new ApproveClaimResponse(
            result.claimId(), result.passportId(), result.assetId(),
            result.qrPublicCode(), result.status()
        );
    }
}

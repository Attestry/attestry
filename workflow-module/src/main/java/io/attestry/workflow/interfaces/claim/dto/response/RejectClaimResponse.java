package io.attestry.workflow.interfaces.claim.dto.response;

import io.attestry.workflow.application.claim.result.RejectPurchaseClaimResult;

public record RejectClaimResponse(String claimId, String status, String rejectionReason) {
    public static RejectClaimResponse from(RejectPurchaseClaimResult result) {
        return new RejectClaimResponse(result.claimId(), result.status(), result.rejectionReason());
    }
}

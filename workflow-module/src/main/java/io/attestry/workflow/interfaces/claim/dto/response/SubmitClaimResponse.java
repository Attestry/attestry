package io.attestry.workflow.interfaces.claim.dto.response;

import io.attestry.workflow.application.claim.result.SubmitPurchaseClaimResult;
import java.time.Instant;

public record SubmitClaimResponse(String claimId, String status, Instant submittedAt) {
    public static SubmitClaimResponse from(SubmitPurchaseClaimResult result) {
        return new SubmitClaimResponse(result.claimId(), result.status(), result.submittedAt());
    }
}

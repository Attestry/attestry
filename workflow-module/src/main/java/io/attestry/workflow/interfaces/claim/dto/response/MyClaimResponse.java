package io.attestry.workflow.interfaces.claim.dto.response;

import io.attestry.workflow.application.claim.result.MyClaimView;
import java.time.Instant;
import java.util.List;

public record MyClaimResponse(
    String claimId,
    String serialNumber, String modelName,
    String status, Instant submittedAt,
    String rejectionReason, String passportId, String assetId,
    List<ClaimEvidenceResponse> evidences
) {
    public static MyClaimResponse from(MyClaimView view) {
        return new MyClaimResponse(
            view.claimId(),
            view.serialNumber(), view.modelName(),
            view.status(), view.submittedAt(),
            view.rejectionReason(), view.passportId(), view.assetId(),
            view.evidences() == null ? List.of() : view.evidences().stream().map(ClaimEvidenceResponse::from).toList()
        );
    }
}

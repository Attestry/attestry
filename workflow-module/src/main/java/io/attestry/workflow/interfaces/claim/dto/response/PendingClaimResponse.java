package io.attestry.workflow.interfaces.claim.dto.response;

import io.attestry.workflow.application.claim.result.PendingClaimView;
import java.time.Instant;

public record PendingClaimResponse(
    String claimId, String claimantUserId,
    String serialNumber, String modelName,
    String evidenceGroupId, String note,
    String status, Instant submittedAt
) {
    public static PendingClaimResponse from(PendingClaimView view) {
        return new PendingClaimResponse(
            view.claimId(), view.claimantUserId(),
            view.serialNumber(), view.modelName(),
            view.evidenceGroupId(), view.note(),
            view.status(), view.submittedAt()
        );
    }
}

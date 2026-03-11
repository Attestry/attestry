package io.attestry.workflow.interfaces.claim.dto.response;

import io.attestry.workflow.application.claim.result.ClaimEvidenceView;
import java.time.Instant;

public record ClaimEvidenceResponse(
    String evidenceId,
    String status,
    String downloadUrl,
    Instant expiresAt
) {
    public static ClaimEvidenceResponse from(ClaimEvidenceView view) {
        return new ClaimEvidenceResponse(
            view.evidenceId(),
            view.status(),
            view.downloadUrl(),
            view.expiresAt()
        );
    }
}

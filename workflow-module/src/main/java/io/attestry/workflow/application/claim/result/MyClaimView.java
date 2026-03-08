package io.attestry.workflow.application.claim.result;

import java.time.Instant;
import java.util.List;

public record MyClaimView(
    String claimId,
    String serialNumber,
    String modelName,
    String status,
    Instant submittedAt,
    String rejectionReason,
    String passportId,
    String assetId,
    List<ClaimEvidenceView> evidences
) {
}

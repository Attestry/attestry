package io.attestry.workflow.application.claim.result;

import java.time.Instant;

public record ClaimEvidenceView(
    String evidenceId,
    String status,
    String downloadUrl,
    Instant expiresAt
) {
}

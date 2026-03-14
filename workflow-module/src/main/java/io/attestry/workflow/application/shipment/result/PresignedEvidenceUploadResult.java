package io.attestry.workflow.application.shipment.result;

import java.time.Instant;

public record PresignedEvidenceUploadResult(
    String evidenceGroupId,
    String evidenceId,
    String objectKey,
    String uploadUrl,
    Instant expiresAt
) {
}

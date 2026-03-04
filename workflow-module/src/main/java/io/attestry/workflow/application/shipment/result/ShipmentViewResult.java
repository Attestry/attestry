package io.attestry.workflow.application.shipment.result;

import java.time.Instant;

public record ShipmentViewResult(
    String shipmentId,
    String tenantId,
    String groupId,
    String passportId,
    int shipmentRound,
    String status,
    Instant releasedAt,
    String releasedByUserId,
    String releasedByGroupId,
    String evidenceGroupId,
    Instant returnedAt,
    String returnedByUserId,
    String returnEvidenceGroupId,
    Instant createdAt
) {
}

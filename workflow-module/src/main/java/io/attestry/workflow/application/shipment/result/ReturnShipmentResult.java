package io.attestry.workflow.application.shipment.result;

import java.time.Instant;
public record ReturnShipmentResult(
    String shipmentId,
    String tenantId,
    String passportId,
    int shipmentRound,
    String status,
    Instant returnedAt,
    String returnedByUserId,
    String returnEvidenceGroupId,
    String outboxEventId
) {
}

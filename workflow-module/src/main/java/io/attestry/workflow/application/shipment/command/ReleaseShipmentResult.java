package io.attestry.workflow.application.shipment.command;

import java.time.Instant;
public record ReleaseShipmentResult(
    String shipmentId,
    String tenantId,
    String passportId,
    int shipmentRound,
    String status,
    Instant releasedAt,
    String evidenceGroupId,
    String outboxEventId
) {
}

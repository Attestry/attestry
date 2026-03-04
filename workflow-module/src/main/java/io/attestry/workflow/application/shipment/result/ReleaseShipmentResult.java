package io.attestry.workflow.application.shipment.result;

import java.time.Instant;
public record ReleaseShipmentResult(
    String shipmentId,
    String tenantId,
    String groupId,
    String passportId,
    int shipmentRound,
    String status,
    Instant releasedAt,
    String evidenceGroupId,
    String outboxEventId
) {
}

package io.attestry.workflow.interfaces.shipment.dto.response;

import io.attestry.workflow.application.shipment.result.ReleaseShipmentResult;
import java.time.Instant;

public record ReleaseShipmentResponse(
        String shipmentId,
        String tenantId,
        String passportId,
        int shipmentRound,
        String status,
        Instant releasedAt,
        String evidenceGroupId,
        String outboxEventId) {
    public static ReleaseShipmentResponse from(ReleaseShipmentResult result) {
        return new ReleaseShipmentResponse(
                result.shipmentId(),
                result.tenantId(),
                result.passportId(),
                result.shipmentRound(),
                result.status(),
                result.releasedAt(),
                result.evidenceGroupId(),
                result.outboxEventId());
    }
}

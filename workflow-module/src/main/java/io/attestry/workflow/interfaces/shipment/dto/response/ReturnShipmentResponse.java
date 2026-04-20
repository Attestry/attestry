package io.attestry.workflow.interfaces.shipment.dto.response;

import io.attestry.workflow.application.shipment.command.ReturnShipmentResult;
import java.time.Instant;

public record ReturnShipmentResponse(
        String shipmentId,
        String tenantId,
        String passportId,
        int shipmentRound,
        String status,
        Instant returnedAt,
        String returnedByUserId,
        String returnEvidenceGroupId,
        String outboxEventId) {
    public static ReturnShipmentResponse from(ReturnShipmentResult result) {
        return new ReturnShipmentResponse(
                result.shipmentId(),
                result.tenantId(),
                result.passportId(),
                result.shipmentRound(),
                result.status(),
                result.returnedAt(),
                result.returnedByUserId(),
                result.returnEvidenceGroupId(),
                result.outboxEventId());
    }
}

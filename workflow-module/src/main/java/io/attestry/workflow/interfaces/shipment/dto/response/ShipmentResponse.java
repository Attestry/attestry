package io.attestry.workflow.interfaces.shipment.dto.response;

import io.attestry.workflow.application.shipment.result.ShipmentViewResult;
import java.time.Instant;

public record ShipmentResponse(
        String shipmentId,
        String passportId,
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        String productionBatch,
        String factoryCode,
        int shipmentRound,
        String status,
        Instant releasedAt,
        Instant returnedAt,
        Instant createdAt) {
    public static ShipmentResponse from(ShipmentViewResult result) {
        return new ShipmentResponse(
                result.shipmentId(),
                result.passportId(),
                result.assetId(),
                result.serialNumber(),
                result.modelId(),
                result.modelName(),
                result.productionBatch(),
                result.factoryCode(),
                result.shipmentRound(),
                result.status(),
                result.releasedAt(),
                result.returnedAt(),
                result.createdAt());
    }
}

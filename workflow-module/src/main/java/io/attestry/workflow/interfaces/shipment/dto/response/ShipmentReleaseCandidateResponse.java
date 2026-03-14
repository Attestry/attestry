package io.attestry.workflow.interfaces.shipment.dto.response;

import io.attestry.workflow.application.shipment.result.ShipmentReleaseCandidateResult;

public record ShipmentReleaseCandidateResponse(
        String passportId,
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        String productionBatch,
        String factoryCode) {
    public static ShipmentReleaseCandidateResponse from(ShipmentReleaseCandidateResult result) {
        return new ShipmentReleaseCandidateResponse(
                result.passportId(),
                result.assetId(),
                result.serialNumber(),
                result.modelId(),
                result.modelName(),
                result.productionBatch(),
                result.factoryCode());
    }
}

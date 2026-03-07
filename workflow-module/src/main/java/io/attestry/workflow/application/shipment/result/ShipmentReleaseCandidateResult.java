package io.attestry.workflow.application.shipment.result;

public record ShipmentReleaseCandidateResult(
    String passportId,
    String assetId,
    String serialNumber,
    String modelId,
    String modelName,
    String productionBatch,
    String factoryCode
) {
}

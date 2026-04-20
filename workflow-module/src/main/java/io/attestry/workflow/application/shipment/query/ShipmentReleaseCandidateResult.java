package io.attestry.workflow.application.shipment.query;

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

package io.attestry.workflow.application.shipment.view;

public record ShipmentReleaseCandidateView(
    String passportId,
    String assetId,
    String serialNumber,
    String modelId,
    String modelName,
    String productionBatch,
    String factoryCode
) {
}

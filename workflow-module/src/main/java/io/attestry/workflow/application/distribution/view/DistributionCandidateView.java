package io.attestry.workflow.application.distribution.view;

public record DistributionCandidateView(
    String passportId,
    String assetId,
    String serialNumber,
    String modelId,
    String modelName,
    String productionBatch,
    String factoryCode
) {
}

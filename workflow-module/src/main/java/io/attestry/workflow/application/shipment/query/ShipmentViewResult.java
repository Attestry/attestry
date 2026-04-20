package io.attestry.workflow.application.shipment.query;

import java.time.Instant;

public record ShipmentViewResult(
    String shipmentId,
    String tenantId,
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
    String releasedByUserId,
    String releasedByTenantId,
    String evidenceGroupId,
    Instant returnedAt,
    String returnedByUserId,
    String returnEvidenceGroupId,
    Instant createdAt
) {
}

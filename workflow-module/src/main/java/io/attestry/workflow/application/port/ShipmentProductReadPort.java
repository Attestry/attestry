package io.attestry.workflow.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ShipmentProductReadPort {

    Optional<PassportState> findPassportState(String passportId);

    Map<String, PassportAssetInfo> findPassportAssetInfoByIds(List<String> passportIds);

    PagedReleaseCandidateResult findReleaseCandidatesByTenantId(
            String tenantId, int page, int size, String keyword);

    PagedShipmentViewResult findShipmentsByTenantId(
            String tenantId, int page, int size, String keyword);

    record PassportState(
        String passportId,
        String tenantId,
        String assetState,
        String riskFlag
    ) {
    }

    record PassportAssetInfo(
        String passportId,
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        String productionBatch,
        String factoryCode
    ) {
    }

    record ShipmentReleaseCandidate(
        String passportId,
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        String productionBatch,
        String factoryCode
    ) {
    }

    record ShipmentJoinedView(
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

    record PagedReleaseCandidateResult(
        List<ShipmentReleaseCandidate> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }

    record PagedShipmentViewResult(
        List<ShipmentJoinedView> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }
}

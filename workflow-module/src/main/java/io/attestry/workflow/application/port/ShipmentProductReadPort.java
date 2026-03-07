package io.attestry.workflow.application.port;

import java.util.List;
import java.util.Optional;

public interface ShipmentProductReadPort {

    Optional<PassportState> findPassportState(String passportId);

    List<ShipmentReleaseCandidate> findReleaseCandidatesByTenantId(String tenantId);

    record PassportState(
        String passportId,
        String tenantId,
        String assetState,
        String riskFlag
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
}

package io.attestry.workflow.application.port;

import java.util.Optional;

public interface ServiceProductReadPort {

    Optional<ServicePassportState> findPassportState(String passportId);

    Optional<String> findCurrentOwnerId(String passportId);

    Optional<ServicePassportAssetInfo> findPassportAssetInfo(String passportId);

    record ServicePassportState(
        String passportId,
        String tenantId,
        String assetState,
        String riskFlag
    ) {
    }

    record ServicePassportAssetInfo(
        String passportId,
        String serialNumber,
        String modelName
    ) {
    }
}

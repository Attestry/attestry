package io.attestry.workflow.application.port;

import java.util.Optional;

public interface ServiceProductReadPort {

    Optional<ServicePassportState> findPassportState(String passportId);

    Optional<String> findCurrentOwnerId(String passportId);

    record ServicePassportState(
        String passportId,
        String tenantId,
        String assetState,
        String riskFlag
    ) {
    }
}

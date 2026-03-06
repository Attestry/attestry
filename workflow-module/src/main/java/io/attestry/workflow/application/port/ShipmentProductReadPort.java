package io.attestry.workflow.application.port;

import java.util.Optional;

public interface ShipmentProductReadPort {

    Optional<PassportState> findPassportState(String passportId);

    record PassportState(
        String passportId,
        String tenantId,
        String assetState,
        String riskFlag
    ) {
    }
}

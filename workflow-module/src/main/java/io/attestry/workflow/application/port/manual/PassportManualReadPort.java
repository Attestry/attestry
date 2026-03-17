package io.attestry.workflow.application.port.manual;

import java.util.Optional;

public interface PassportManualReadPort {

    Optional<PassportManualContext> findContext(String passportId);

    record PassportManualContext(
        String passportId,
        String tenantId,
        String serialNumber,
        String modelName,
        String ownerUserId
    ) {
    }
}

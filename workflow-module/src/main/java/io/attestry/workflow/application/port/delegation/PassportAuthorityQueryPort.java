package io.attestry.workflow.application.port.delegation;

import java.util.Optional;

public interface PassportAuthorityQueryPort {

    Optional<PassportAuthorityRecord> findPassportAuthority(String passportId);

    record PassportAuthorityRecord(
        String passportId,
        String tenantId,
        String assetState,
        String riskFlag
    ) {
    }
}

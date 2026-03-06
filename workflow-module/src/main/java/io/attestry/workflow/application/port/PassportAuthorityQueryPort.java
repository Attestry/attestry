package io.attestry.workflow.application.port;

import java.util.Optional;

public interface PassportAuthorityQueryPort {

    Optional<PassportAuthorityView> findPassportAuthority(String passportId);

    record PassportAuthorityView(
        String passportId,
        String tenantId,
        String assetState,
        String riskFlag
    ) {
    }
}


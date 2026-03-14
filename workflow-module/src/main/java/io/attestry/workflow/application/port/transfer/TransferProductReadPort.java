package io.attestry.workflow.application.port.transfer;

import java.util.Optional;

public interface TransferProductReadPort {

    Optional<TransferPassportState> findPassportState(String passportId);

    Optional<String> findCurrentOwnerId(String passportId);

    boolean hasRetailPermission(String passportId, String sellerTenantId);

    record TransferPassportState(
        String passportId,
        String tenantId,
        String assetState,
        String riskFlag
    ) {
    }
}

package io.attestry.workflow.application.port;

import java.util.Optional;

public interface TransferProductReadPort {

    Optional<TransferPassportState> findPassportState(String passportId);

    Optional<String> findCurrentOwnerId(String passportId);

    boolean hasRetailPermission(String passportId, String sellerGroupId);

    record TransferPassportState(
        String passportId,
        String tenantId,
        String groupId,
        String assetState,
        String riskFlag
    ) {
    }
}

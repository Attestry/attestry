package io.attestry.workflow.application.port.transfer;

import java.time.Instant;

public interface TransferOwnershipUpdatePort {

    void upsertOwner(String passportId, String newOwnerId, Instant updatedAt);
}

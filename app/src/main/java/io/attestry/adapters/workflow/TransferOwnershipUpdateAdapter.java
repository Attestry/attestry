package io.attestry.adapters.workflow;

import io.attestry.product.application.port.ownership.OwnershipUpdatePort;
import io.attestry.workflow.application.port.projection.WorkflowPassportProjectionWritePort;
import io.attestry.workflow.application.port.transfer.TransferOwnershipUpdatePort;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class TransferOwnershipUpdateAdapter implements TransferOwnershipUpdatePort {

    private final OwnershipUpdatePort ownershipUpdatePort;
    private final WorkflowPassportProjectionWritePort projectionWriter;

    public TransferOwnershipUpdateAdapter(
        OwnershipUpdatePort ownershipUpdatePort,
        WorkflowPassportProjectionWritePort projectionWriter
    ) {
        this.ownershipUpdatePort = ownershipUpdatePort;
        this.projectionWriter = projectionWriter;
    }

    @Override
    public void upsertOwner(String passportId, String newOwnerId, Instant updatedAt) {
        ownershipUpdatePort.updateOwner(passportId, newOwnerId);
        projectionWriter.upsertOwnership(
            passportId,
            newOwnerId,
            "ownership:" + passportId + ":" + updatedAt.toEpochMilli(),
            null,
            updatedAt
        );
    }
}

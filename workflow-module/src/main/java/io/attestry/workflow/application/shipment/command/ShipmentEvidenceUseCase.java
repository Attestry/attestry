package io.attestry.workflow.application.shipment.command;

import io.attestry.workflow.application.common.WorkflowActorContext;

public interface ShipmentEvidenceUseCase {

    PresignedEvidenceUploadResult presignEvidenceUpload(
        WorkflowActorContext principal,
        PresignShipmentEvidenceUploadCommand command
    );

    EvidenceCompleteResult completeEvidenceUpload(
        WorkflowActorContext principal,
        CompleteShipmentEvidenceUploadCommand command
    );
}

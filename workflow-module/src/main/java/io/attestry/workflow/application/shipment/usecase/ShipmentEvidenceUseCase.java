package io.attestry.workflow.application.shipment.usecase;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.result.PresignedEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.EvidenceCompleteResult;

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

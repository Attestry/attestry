package io.attestry.workflow.application.manual.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.EvidenceCompleteResult;
import io.attestry.workflow.application.shipment.command.PresignedEvidenceUploadResult;

public interface PassportManualEvidenceUseCase {

    PresignedEvidenceUploadResult presignEvidenceUpload(
        WorkflowActorContext principal,
        String tenantId,
        PresignShipmentEvidenceUploadCommand command
    );

    EvidenceCompleteResult completeEvidenceUpload(
        WorkflowActorContext principal,
        String tenantId,
        CompleteShipmentEvidenceUploadCommand command
    );
}

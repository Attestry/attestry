package io.attestry.workflow.application.manual.usecase;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.result.EvidenceCompleteResult;
import io.attestry.workflow.application.shipment.result.PresignedEvidenceUploadResult;

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

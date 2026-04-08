package io.attestry.workflow.application.servicerequest.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.result.PresignedEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.EvidenceCompleteResult;

public interface ServiceEvidenceUseCase {

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

    PresignedEvidenceUploadResult presignOwnerEvidenceUpload(
        WorkflowActorContext principal,
        PresignShipmentEvidenceUploadCommand command
    );

    EvidenceCompleteResult completeOwnerEvidenceUpload(
        WorkflowActorContext principal,
        CompleteShipmentEvidenceUploadCommand command
    );
}

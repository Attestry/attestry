package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.result.PresignedEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.EvidenceCompleteResult;

public interface ShipmentEvidenceUseCase {

    PresignedEvidenceUploadResult presignEvidenceUpload(
        AuthPrincipal principal,
        String tenantId,
        PresignShipmentEvidenceUploadCommand command
    );

    EvidenceCompleteResult completeEvidenceUpload(
        AuthPrincipal principal,
        String tenantId,
        CompleteShipmentEvidenceUploadCommand command
    );
}

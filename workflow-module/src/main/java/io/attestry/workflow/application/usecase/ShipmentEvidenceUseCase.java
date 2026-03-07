package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.result.PresignedShipmentEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceCompleteResult;

public interface ShipmentEvidenceUseCase {

    PresignedShipmentEvidenceUploadResult presignEvidenceUpload(
        AuthPrincipal principal,
        PresignShipmentEvidenceUploadCommand command
    );

    ShipmentEvidenceCompleteResult completeEvidenceUpload(
        AuthPrincipal principal,
        CompleteShipmentEvidenceUploadCommand command
    );
}

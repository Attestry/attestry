package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.shipment.command.ReleaseShipmentCommand;
import io.attestry.workflow.application.shipment.command.ReturnShipmentCommand;
import io.attestry.workflow.application.shipment.result.ReleaseShipmentResult;
import io.attestry.workflow.application.shipment.result.ReturnShipmentResult;

public interface ShipmentReleaseUseCase {

    ReleaseShipmentResult release(
        AuthPrincipal principal,
        String passportId,
        ReleaseShipmentCommand command
    );

    ReturnShipmentResult returnShipment(
        AuthPrincipal principal,
        String shipmentId,
        ReturnShipmentCommand command
    );
}

package io.attestry.workflow.application.shipment.command;

import io.attestry.workflow.application.common.WorkflowActorContext;

public interface ShipmentReleaseUseCase {

    ReleaseShipmentResult release(
        WorkflowActorContext principal,
        String passportId,
        ReleaseShipmentCommand command
    );

    ReturnShipmentResult returnShipment(
        WorkflowActorContext principal,
        String shipmentId,
        ReturnShipmentCommand command
    );
}

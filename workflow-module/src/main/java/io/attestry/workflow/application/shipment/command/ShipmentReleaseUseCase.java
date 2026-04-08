package io.attestry.workflow.application.shipment.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.shipment.command.ReleaseShipmentCommand;
import io.attestry.workflow.application.shipment.command.ReturnShipmentCommand;
import io.attestry.workflow.application.shipment.result.ReleaseShipmentResult;
import io.attestry.workflow.application.shipment.result.ReturnShipmentResult;

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

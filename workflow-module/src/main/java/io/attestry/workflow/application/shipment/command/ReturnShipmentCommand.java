package io.attestry.workflow.application.shipment.command;

public record ReturnShipmentCommand(
    String returnEvidenceGroupId,
    String reason
) {
}

package io.attestry.workflow.application.shipment.command;

public record PresignShipmentEvidenceUploadCommand(
    String evidenceGroupId,
    String fileName,
    String contentType
) {
}

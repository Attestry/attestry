package io.attestry.workflow.application.shipment.command;

public record CompleteShipmentEvidenceUploadCommand(
    String evidenceGroupId,
    String evidenceId,
    long sizeBytes,
    String fileHash
) {
}

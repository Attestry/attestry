package io.attestry.workflow.application.shipment.result;

public record ShipmentEvidenceViewResult(
    String evidenceId,
    String evidenceGroupId,
    String fileHash
) {
}

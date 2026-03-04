package io.attestry.workflow.application.shipment.result;

public record ShipmentEvidenceCompleteResult(
    String evidenceGroupId,
    String evidenceId,
    String status
) {
}

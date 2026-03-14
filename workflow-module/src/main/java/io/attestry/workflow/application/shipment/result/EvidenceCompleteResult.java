package io.attestry.workflow.application.shipment.result;

public record EvidenceCompleteResult(
    String evidenceGroupId,
    String evidenceId,
    String status
) {
}

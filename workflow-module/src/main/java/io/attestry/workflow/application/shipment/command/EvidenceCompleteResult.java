package io.attestry.workflow.application.shipment.command;

public record EvidenceCompleteResult(
    String evidenceGroupId,
    String evidenceId,
    String status
) {
}

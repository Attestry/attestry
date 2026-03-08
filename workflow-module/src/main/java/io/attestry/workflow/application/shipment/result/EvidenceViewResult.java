package io.attestry.workflow.application.shipment.result;

public record EvidenceViewResult(
    String evidenceId,
    String evidenceGroupId,
    String fileHash
) {
}

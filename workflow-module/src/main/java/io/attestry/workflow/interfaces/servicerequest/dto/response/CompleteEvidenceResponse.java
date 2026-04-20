package io.attestry.workflow.interfaces.servicerequest.dto.response;

import io.attestry.workflow.application.shipment.command.EvidenceCompleteResult;

public record CompleteEvidenceResponse(
    String evidenceGroupId,
    String evidenceId,
    String status
) {
    public static CompleteEvidenceResponse from(EvidenceCompleteResult result) {
        return new CompleteEvidenceResponse(result.evidenceGroupId(), result.evidenceId(), result.status());
    }
}

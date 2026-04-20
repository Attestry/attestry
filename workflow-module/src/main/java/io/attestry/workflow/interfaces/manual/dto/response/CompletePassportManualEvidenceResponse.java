package io.attestry.workflow.interfaces.manual.dto.response;

import io.attestry.workflow.application.shipment.command.EvidenceCompleteResult;

public record CompletePassportManualEvidenceResponse(
    String evidenceGroupId,
    String evidenceId,
    String status
) {

    public static CompletePassportManualEvidenceResponse from(EvidenceCompleteResult result) {
        return new CompletePassportManualEvidenceResponse(
            result.evidenceGroupId(),
            result.evidenceId(),
            result.status()
        );
    }
}

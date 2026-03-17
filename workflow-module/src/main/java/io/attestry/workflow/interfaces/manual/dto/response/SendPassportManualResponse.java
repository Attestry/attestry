package io.attestry.workflow.interfaces.manual.dto.response;

import io.attestry.workflow.application.manual.result.SendPassportManualResult;

public record SendPassportManualResponse(
    String passportId,
    String recipientEmailMasked,
    String evidenceGroupId,
    boolean hasAttachment
) {

    public static SendPassportManualResponse from(SendPassportManualResult result) {
        return new SendPassportManualResponse(
            result.passportId(),
            result.recipientEmailMasked(),
            result.evidenceGroupId(),
            result.hasAttachment()
        );
    }
}

package io.attestry.workflow.interfaces.manual.dto.response;

import io.attestry.workflow.application.manual.result.PassportManualRecipientResult;

public record PassportManualRecipientResponse(
    boolean available,
    String message,
    String recipientEmailMasked
) {

    public static PassportManualRecipientResponse from(PassportManualRecipientResult result) {
        return new PassportManualRecipientResponse(
            result.available(),
            result.message(),
            result.recipientEmailMasked()
        );
    }
}

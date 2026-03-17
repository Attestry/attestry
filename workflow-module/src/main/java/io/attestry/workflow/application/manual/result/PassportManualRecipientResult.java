package io.attestry.workflow.application.manual.result;

public record PassportManualRecipientResult(
    boolean available,
    String message,
    String recipientEmailMasked
) {
}

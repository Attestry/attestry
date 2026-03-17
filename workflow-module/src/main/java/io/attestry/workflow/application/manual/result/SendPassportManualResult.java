package io.attestry.workflow.application.manual.result;

public record SendPassportManualResult(
    String passportId,
    String recipientEmailMasked,
    String evidenceGroupId,
    boolean hasAttachment
) {
}

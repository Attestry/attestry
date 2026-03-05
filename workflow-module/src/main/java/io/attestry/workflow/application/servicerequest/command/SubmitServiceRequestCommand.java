package io.attestry.workflow.application.servicerequest.command;

public record SubmitServiceRequestCommand(
    String passportId,
    String serviceType,
    String description,
    String beforeEvidenceGroupId
) {
}

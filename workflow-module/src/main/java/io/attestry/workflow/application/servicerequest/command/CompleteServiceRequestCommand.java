package io.attestry.workflow.application.servicerequest.command;

public record CompleteServiceRequestCommand(
    String afterEvidenceGroupId,
    String serviceResult
) {
}

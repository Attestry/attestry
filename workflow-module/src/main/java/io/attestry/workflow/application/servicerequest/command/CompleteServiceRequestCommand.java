package io.attestry.workflow.application.servicerequest.command;

public record CompleteServiceRequestCommand(
    String serviceType,
    String afterEvidenceGroupId,
    String serviceResult,
    String completionMemo
) {
}

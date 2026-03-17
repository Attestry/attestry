package io.attestry.workflow.application.manual.command;

public record SendPassportManualCommand(
    String message,
    String evidenceGroupId
) {
}

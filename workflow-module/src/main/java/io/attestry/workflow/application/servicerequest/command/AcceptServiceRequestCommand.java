package io.attestry.workflow.application.servicerequest.command;

public record AcceptServiceRequestCommand(
    String serviceType,
    String description
) {
}

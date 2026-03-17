package io.attestry.workflow.application.manual.command;

import java.util.List;

public record SendPassportManualCommand(
    List<String> passportIds,
    String message,
    String evidenceGroupId
) {
}

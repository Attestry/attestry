package io.attestry.workflow.application.delegation.command;

import java.time.Instant;
import java.util.List;

public record BatchGrantPassportDelegationCommand(
    List<String> passportIds,
    Instant expiresAt,
    String note
) {
}

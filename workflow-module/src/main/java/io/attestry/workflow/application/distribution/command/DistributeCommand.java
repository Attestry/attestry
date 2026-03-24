package io.attestry.workflow.application.distribution.command;

import java.time.Instant;
import java.util.List;

public record DistributeCommand(
    List<String> passportIds,
    Instant expiresAt,
    String note
) {
}

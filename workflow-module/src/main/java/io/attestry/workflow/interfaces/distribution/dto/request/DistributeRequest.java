package io.attestry.workflow.interfaces.distribution.dto.request;

import java.time.Instant;
import java.util.List;

public record DistributeRequest(
    List<String> passportIds,
    Instant expiresAt,
    String note
) {
}

package io.attestry.workflow.application.servicerequest.result;

import java.time.Instant;

public record CompleteServiceRequestResult(
    String serviceRequestId,
    String passportId,
    String status,
    Instant completedAt,
    String outboxEventId
) {
}

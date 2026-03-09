package io.attestry.workflow.application.servicerequest.result;

import java.time.Instant;

public record AcceptServiceRequestResult(
    String serviceRequestId,
    String passportId,
    String status,
    Instant acceptedAt
) {
}

package io.attestry.workflow.application.servicerequest.result;

import java.time.Instant;

public record RejectServiceRequestResult(
    String serviceRequestId,
    String passportId,
    String status,
    Instant rejectedAt
) {
}

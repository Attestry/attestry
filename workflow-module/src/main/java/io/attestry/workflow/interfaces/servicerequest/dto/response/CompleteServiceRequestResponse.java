package io.attestry.workflow.interfaces.servicerequest.dto.response;

import io.attestry.workflow.application.servicerequest.result.CompleteServiceRequestResult;
import java.time.Instant;

public record CompleteServiceRequestResponse(
    String serviceRequestId,
    String passportId,
    String status,
    Instant completedAt,
    String outboxEventId
) {
    public static CompleteServiceRequestResponse from(CompleteServiceRequestResult result) {
        return new CompleteServiceRequestResponse(
            result.serviceRequestId(),
            result.passportId(),
            result.status(),
            result.completedAt(),
            result.outboxEventId()
        );
    }
}

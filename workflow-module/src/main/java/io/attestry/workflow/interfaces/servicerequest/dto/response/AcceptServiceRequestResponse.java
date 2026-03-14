package io.attestry.workflow.interfaces.servicerequest.dto.response;

import io.attestry.workflow.application.servicerequest.result.AcceptServiceRequestResult;
import java.time.Instant;

public record AcceptServiceRequestResponse(
    String serviceRequestId,
    String passportId,
    String status,
    Instant acceptedAt
) {
    public static AcceptServiceRequestResponse from(AcceptServiceRequestResult result) {
        return new AcceptServiceRequestResponse(
            result.serviceRequestId(),
            result.passportId(),
            result.status(),
            result.acceptedAt()
        );
    }
}

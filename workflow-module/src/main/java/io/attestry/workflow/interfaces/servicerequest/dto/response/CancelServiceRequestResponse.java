package io.attestry.workflow.interfaces.servicerequest.dto.response;

import io.attestry.workflow.application.servicerequest.result.CancelServiceRequestResult;
import java.time.Instant;

public record CancelServiceRequestResponse(
    String serviceRequestId,
    String passportId,
    String status,
    Instant cancelledAt
) {
    public static CancelServiceRequestResponse from(CancelServiceRequestResult result) {
        return new CancelServiceRequestResponse(
            result.serviceRequestId(),
            result.passportId(),
            result.status(),
            result.cancelledAt()
        );
    }
}

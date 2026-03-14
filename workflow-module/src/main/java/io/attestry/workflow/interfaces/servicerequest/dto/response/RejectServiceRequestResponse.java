package io.attestry.workflow.interfaces.servicerequest.dto.response;

import io.attestry.workflow.application.servicerequest.result.RejectServiceRequestResult;
import java.time.Instant;

public record RejectServiceRequestResponse(
    String serviceRequestId,
    String passportId,
    String status,
    Instant rejectedAt
) {
    public static RejectServiceRequestResponse from(RejectServiceRequestResult result) {
        return new RejectServiceRequestResponse(
            result.serviceRequestId(),
            result.passportId(),
            result.status(),
            result.rejectedAt()
        );
    }
}

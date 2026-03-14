package io.attestry.workflow.interfaces.servicerequest.dto.response;

import io.attestry.workflow.application.servicerequest.result.SubmitServiceRequestResult;
import java.time.Instant;

public record SubmitServiceRequestResponse(
    String serviceRequestId,
    String passportId,
    String providerTenantId,
    String serviceType,
    String status,
    String permissionId,
    Instant submittedAt
) {
    public static SubmitServiceRequestResponse from(SubmitServiceRequestResult result) {
        return new SubmitServiceRequestResponse(
            result.serviceRequestId(),
            result.passportId(),
            result.providerTenantId(),
            result.serviceType(),
            result.status(),
            result.permissionId(),
            result.submittedAt()
        );
    }
}

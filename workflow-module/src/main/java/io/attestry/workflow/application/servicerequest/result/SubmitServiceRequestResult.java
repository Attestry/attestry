package io.attestry.workflow.application.servicerequest.result;

import java.time.Instant;

public record SubmitServiceRequestResult(
    String serviceRequestId,
    String passportId,
    String providerTenantId,
    String serviceType,
    String status,
    String permissionId,
    Instant submittedAt
) {
}

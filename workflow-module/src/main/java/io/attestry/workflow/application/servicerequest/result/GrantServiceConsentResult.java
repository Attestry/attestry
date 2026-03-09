package io.attestry.workflow.application.servicerequest.result;

import java.time.Instant;

public record GrantServiceConsentResult(
    String permissionId,
    String serviceRequestId,
    String passportId,
    String providerTenantId,
    String consentStatus,
    String serviceRequestStatus,
    Instant grantedAt
) {
}

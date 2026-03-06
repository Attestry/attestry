package io.attestry.workflow.application.servicerequest.result;

import java.time.Instant;

public record GrantServiceConsentResult(
    String permissionId,
    String passportId,
    String providerTenantId,
    String status,
    Instant grantedAt
) {
}

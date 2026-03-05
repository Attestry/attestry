package io.attestry.workflow.application.servicerequest.result;

import java.time.Instant;

public record GrantServiceConsentResult(
    String permissionId,
    String passportId,
    String providerGroupId,
    String status,
    Instant grantedAt
) {
}

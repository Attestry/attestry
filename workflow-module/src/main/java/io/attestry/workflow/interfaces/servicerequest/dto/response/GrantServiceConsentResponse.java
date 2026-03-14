package io.attestry.workflow.interfaces.servicerequest.dto.response;

import io.attestry.workflow.application.servicerequest.result.GrantServiceConsentResult;
import java.time.Instant;

public record GrantServiceConsentResponse(
    String permissionId,
    String serviceRequestId,
    String passportId,
    String providerTenantId,
    String consentStatus,
    String serviceRequestStatus,
    Instant grantedAt
) {
    public static GrantServiceConsentResponse from(GrantServiceConsentResult result) {
        return new GrantServiceConsentResponse(
            result.permissionId(),
            result.serviceRequestId(),
            result.passportId(),
            result.providerTenantId(),
            result.consentStatus(),
            result.serviceRequestStatus(),
            result.grantedAt()
        );
    }
}

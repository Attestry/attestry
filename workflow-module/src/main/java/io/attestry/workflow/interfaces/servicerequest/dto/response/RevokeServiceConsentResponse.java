package io.attestry.workflow.interfaces.servicerequest.dto.response;

import io.attestry.workflow.application.servicerequest.result.RevokeServiceConsentResult;

public record RevokeServiceConsentResponse(
    String passportId,
    String providerTenantId,
    String status
) {
    public static RevokeServiceConsentResponse from(RevokeServiceConsentResult result) {
        return new RevokeServiceConsentResponse(
            result.passportId(),
            result.providerTenantId(),
            result.status()
        );
    }
}

package io.attestry.workflow.application.servicerequest.result;

public record RevokeServiceConsentResult(
    String passportId,
    String providerTenantId,
    String status
) {
}

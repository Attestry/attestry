package io.attestry.workflow.interfaces.servicerequest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RevokeServiceConsentRequest(
    @NotBlank(message = "Provider tenant ID is required")
    String providerTenantId
) {
}

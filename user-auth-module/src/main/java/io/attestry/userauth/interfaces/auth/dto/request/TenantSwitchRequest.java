package io.attestry.userauth.interfaces.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TenantSwitchRequest(
    @NotBlank(message = "Membership ID is required")
    String membershipId
) {
}

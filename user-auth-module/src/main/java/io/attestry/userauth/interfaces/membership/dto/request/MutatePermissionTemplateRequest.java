package io.attestry.userauth.interfaces.membership.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MutatePermissionTemplateRequest(
        @NotBlank
        String reason
) {
}

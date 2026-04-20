package io.attestry.userauth.interfaces.template.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePermissionTemplateRequest(
    @NotBlank(message = "Template code is required")
    String code,
    @NotBlank(message = "Template name is required")
    String name,
    String description
) {
}

package io.attestry.userauth.interfaces.template.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdatePermissionTemplateRequest(
    @NotBlank(message = "Template name is required")
    String name,
    String description,
    Boolean enabled
) {
}

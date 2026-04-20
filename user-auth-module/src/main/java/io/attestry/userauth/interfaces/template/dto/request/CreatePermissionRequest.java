package io.attestry.userauth.interfaces.template.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePermissionRequest(
    @NotBlank(message = "Permission code is required")
    String code,
    @NotBlank(message = "Permission name is required")
    String name,
    String description,
    @NotBlank(message = "Resource type is required")
    String resourceType,
    @NotBlank(message = "Action is required")
    String action
) {
}

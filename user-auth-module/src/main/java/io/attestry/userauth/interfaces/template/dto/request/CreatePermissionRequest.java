package io.attestry.userauth.interfaces.template.dto.request;

public record CreatePermissionRequest(
    String code,
    String name,
    String description,
    String resourceType,
    String action
) {
}

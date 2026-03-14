package io.attestry.userauth.application.dto.result;

public record PermissionResult(
    String permissionId,
    String code,
    String name,
    String description,
    String resourceType,
    String action,
    boolean enabled
) {
}

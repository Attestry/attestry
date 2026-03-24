package io.attestry.userauth.application.template.result;

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

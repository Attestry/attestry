package io.attestry.userauth.interfaces.template.dto.response;

import io.attestry.userauth.application.dto.result.PermissionResult;

public record PermissionResponse(
    String permissionId,
    String code,
    String name,
    String description,
    String resourceType,
    String action,
    boolean enabled
) {
    public static PermissionResponse from(PermissionResult result) {
        return new PermissionResponse(
            result.permissionId(),
            result.code(),
            result.name(),
            result.description(),
            result.resourceType(),
            result.action(),
            result.enabled()
        );
    }
}

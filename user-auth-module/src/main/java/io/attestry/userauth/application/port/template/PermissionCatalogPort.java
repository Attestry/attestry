package io.attestry.userauth.application.port.template;

import java.util.List;

public interface PermissionCatalogPort {

    PermissionView createPermission(
            String code,
            String name,
            String description,
            String resourceType,
            String action);

    List<PermissionView> findAllPermissions();

    record PermissionView(
            String permissionId,
            String code,
            String name,
            String description,
            String resourceType,
            String action,
            boolean enabled) {
    }
}

package io.attestry.userauth.domain.auth.policy;

import java.util.Optional;
import java.util.Set;

public interface PermissionCatalog {

    Optional<PermissionDefinition> findByCode(String permissionCode);

    default boolean isKnown(String permissionCode) {
        return findByCode(permissionCode).isPresent();
    }

    Set<PermissionDefinition> all();

    record PermissionDefinition(
        String permissionId,
        String code,
        String name,
        String description,
        String resourceType,
        String action,
        boolean enabled
    ) {
    }
}

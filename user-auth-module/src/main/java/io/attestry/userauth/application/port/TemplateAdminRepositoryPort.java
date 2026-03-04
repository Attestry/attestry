package io.attestry.userauth.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TemplateAdminRepositoryPort {

    Optional<PermissionTemplateView> findTemplateByCode(String templateCode);

    Optional<PermissionTemplateView> findTemplateByCodeAndTenantId(String templateCode, String tenantId);

    Optional<PermissionTemplateView> findTemplateVisibleToTenant(String templateCode, String tenantId);

    List<PermissionTemplateView> findAllTemplates();

    List<PermissionTemplateView> findTemplatesVisibleToTenant(String tenantId);

    PermissionTemplateView createTemplate(
        String code,
        String name,
        String description,
        String tenantId,
        String actorUserId,
        Instant now
    );

    PermissionTemplateView updateTemplateMeta(
        String code,
        String tenantId,
        String name,
        String description,
        Boolean enabled,
        Instant now
    );

    PermissionView createPermission(
        String code,
        String name,
        String description,
        String resourceType,
        String action
    );

    List<PermissionView> findAllPermissions();

    Set<String> replaceTemplatePermissions(String templateCode, String tenantId, Set<String> permissionCodes);

    Set<String> addTemplatePermissions(String templateCode, String tenantId, Set<String> permissionCodes);

    Set<String> removeTemplatePermission(String templateCode, String tenantId, String permissionCode);

    TenantRoleTemplateBindingView bindTemplateToTenantRole(
        String tenantId,
        String roleCode,
        String templateCode,
        String actorUserId,
        Instant now
    );

    List<TenantRoleTemplateBindingView> findTenantRoleTemplateBindings(String tenantId);

    void disableTenantRoleTemplateBinding(String tenantId, String roleCode, String templateCode, Instant now);

    record PermissionTemplateView(
        String templateId,
        String tenantId,
        String code,
        String name,
        String description,
        boolean enabled,
        List<String> permissionCodes
    ) {
    }

    record TenantRoleTemplateBindingView(
        String bindingId,
        String tenantId,
        String roleCode,
        String templateCode,
        boolean enabled
    ) {
    }

    record PermissionView(
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

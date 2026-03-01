package io.attestry.userauth.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TemplateAdminRepositoryPort {

    Optional<PermissionTemplateView> findTemplateByCode(String templateCode);

    List<PermissionTemplateView> findAllTemplates();

    PermissionTemplateView createTemplate(
        String code,
        String name,
        String description,
        String actorUserId,
        Instant now
    );

    PermissionTemplateView updateTemplateMeta(
        String code,
        String name,
        String description,
        Boolean enabled,
        Instant now
    );

    Set<String> replaceTemplatePermissions(String templateCode, Set<String> permissionCodes);

    Set<String> addTemplatePermissions(String templateCode, Set<String> permissionCodes);

    Set<String> removeTemplatePermission(String templateCode, String permissionCode);

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
}

package io.attestry.userauth.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PermissionTemplatePort {

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
            Instant now);

    PermissionTemplateView updateTemplateMeta(
            String code,
            String tenantId,
            String name,
            String description,
            Boolean enabled,
            Instant now);

    Set<String> replaceTemplatePermissions(String templateCode, String tenantId, Set<String> permissionCodes);

    Set<String> addTemplatePermissions(String templateCode, String tenantId, Set<String> permissionCodes);

    Set<String> removeTemplatePermission(String templateCode, String tenantId, String permissionCode);

    record PermissionTemplateView(
            String templateId,
            String tenantId,
            String code,
            String name,
            String description,
            boolean enabled,
            List<String> permissionCodes) {
    }
}

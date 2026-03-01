package io.attestry.userauth.application.usecase.membership;

import io.attestry.userauth.application.dto.result.PermissionTemplateResult;
import io.attestry.userauth.application.dto.result.TenantRoleTemplateBindingResult;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import java.util.List;

public interface TemplateAdminUseCase {

    PermissionTemplateResult createTemplate(AuthPrincipal principal, CreateTemplateCommand command);

    List<PermissionTemplateResult> listTemplates(AuthPrincipal principal);

    PermissionTemplateResult getTemplate(AuthPrincipal principal, String templateCode);

    PermissionTemplateResult updateTemplate(AuthPrincipal principal, String templateCode, UpdateTemplateCommand command);

    PermissionTemplateResult replaceTemplatePermissions(
        AuthPrincipal principal,
        String templateCode,
        SetTemplatePermissionsCommand command
    );

    PermissionTemplateResult addTemplatePermissions(
        AuthPrincipal principal,
        String templateCode,
        AddTemplatePermissionsCommand command
    );

    PermissionTemplateResult removeTemplatePermission(AuthPrincipal principal, String templateCode, String permissionCode);

    TenantRoleTemplateBindingResult bindTenantRoleTemplate(
        AuthPrincipal principal,
        String tenantId,
        BindTenantRoleTemplateCommand command
    );

    List<TenantRoleTemplateBindingResult> listTenantRoleTemplateBindings(AuthPrincipal principal, String tenantId);

    void unbindTenantRoleTemplate(AuthPrincipal principal, String tenantId, String roleCode, String templateCode);

    record CreateTemplateCommand(String code, String name, String description) {
    }

    record UpdateTemplateCommand(String name, String description, Boolean enabled) {
    }

    record SetTemplatePermissionsCommand(List<String> permissionCodes) {
    }

    record AddTemplatePermissionsCommand(List<String> permissionCodes) {
    }

    record BindTenantRoleTemplateCommand(String roleCode, String templateCode) {
    }
}

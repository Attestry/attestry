package io.attestry.userauth.application.usecase.membership;

import io.attestry.userauth.application.dto.result.PermissionTemplateResult;
import io.attestry.userauth.application.dto.result.TenantRoleTemplateBindingResult;
import io.attestry.userauth.application.dto.command.ActorContext;
import java.util.List;

public interface TemplateAdminUseCase {

    PermissionTemplateResult createTemplate(ActorContext actor, CreateTemplateCommand command);

    List<PermissionTemplateResult> listTemplates(ActorContext actor);

    PermissionTemplateResult getTemplate(ActorContext actor, String templateCode);

    PermissionTemplateResult updateTemplate(ActorContext actor, String templateCode, UpdateTemplateCommand command);

    PermissionTemplateResult replaceTemplatePermissions(
        ActorContext actor,
        String templateCode,
        SetTemplatePermissionsCommand command
    );

    PermissionTemplateResult addTemplatePermissions(
        ActorContext actor,
        String templateCode,
        AddTemplatePermissionsCommand command
    );

    PermissionTemplateResult removeTemplatePermission(ActorContext actor, String templateCode, String permissionCode);

    TenantRoleTemplateBindingResult bindTenantRoleTemplate(
        ActorContext actor,
        String tenantId,
        BindTenantRoleTemplateCommand command
    );

    List<TenantRoleTemplateBindingResult> listTenantRoleTemplateBindings(ActorContext actor, String tenantId);

    void unbindTenantRoleTemplate(ActorContext actor, String tenantId, String roleCode, String templateCode);

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

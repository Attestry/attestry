package io.attestry.userauth.application.usecase.template;

import io.attestry.userauth.application.dto.result.PermissionTemplateResult;
import io.attestry.userauth.application.dto.result.PermissionResult;
import io.attestry.userauth.application.dto.result.TenantRoleTemplateBindingResult;
import io.attestry.userauth.application.dto.command.ActorContext;
import java.util.List;

public interface TemplateManagementUseCase {

    PermissionTemplateResult createTemplate(ActorContext actor, CreateTemplateCommand command);

    PermissionResult createPermission(ActorContext actor, CreatePermissionCommand command);

    List<PermissionResult> listPermissions(ActorContext actor);

    PermissionTemplateResult createTenantTemplate(ActorContext actor, String tenantId, CreateTemplateCommand command);

    List<PermissionTemplateResult> listTemplates(ActorContext actor);

    List<PermissionTemplateResult> listTenantTemplates(ActorContext actor, String tenantId);

    PermissionTemplateResult getTemplate(ActorContext actor, String templateCode);

    PermissionTemplateResult getTenantTemplate(ActorContext actor, String tenantId, String templateCode);

    PermissionTemplateResult updateTemplate(ActorContext actor, String templateCode, UpdateTemplateCommand command);

    PermissionTemplateResult updateTenantTemplate(ActorContext actor, String tenantId, String templateCode, UpdateTemplateCommand command);

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

    PermissionTemplateResult replaceTenantTemplatePermissions(
        ActorContext actor,
        String tenantId,
        String templateCode,
        SetTemplatePermissionsCommand command
    );

    PermissionTemplateResult addTenantTemplatePermissions(
        ActorContext actor,
        String tenantId,
        String templateCode,
        AddTemplatePermissionsCommand command
    );

    PermissionTemplateResult removeTenantTemplatePermission(
        ActorContext actor,
        String tenantId,
        String templateCode,
        String permissionCode
    );

    TenantRoleTemplateBindingResult bindTenantRoleTemplate(
        ActorContext actor,
        String tenantId,
        BindTenantRoleTemplateCommand command
    );

    List<TenantRoleTemplateBindingResult> listTenantRoleTemplateBindings(ActorContext actor, String tenantId);

    void unbindTenantRoleTemplate(ActorContext actor, String tenantId, String roleCode, String templateCode);

    record CreateTemplateCommand(String code, String name, String description) {
    }

    record CreatePermissionCommand(
        String code,
        String name,
        String description,
        String resourceType,
        String action
    ) {
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

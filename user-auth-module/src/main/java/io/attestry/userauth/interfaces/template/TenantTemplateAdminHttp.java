package io.attestry.userauth.interfaces.template;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.result.PermissionTemplateResult;
import io.attestry.userauth.application.dto.result.TenantRoleTemplateBindingResult;
import io.attestry.userauth.application.usecase.membership.TemplateAdminUseCase;
import io.attestry.userauth.interfaces.template.dto.request.BindTenantRoleTemplateRequest;
import io.attestry.userauth.interfaces.template.dto.request.CreatePermissionTemplateRequest;
import io.attestry.userauth.interfaces.template.dto.request.SetTemplatePermissionsRequest;
import io.attestry.userauth.interfaces.template.dto.request.UpdatePermissionTemplateRequest;
import io.attestry.userauth.interfaces.template.dto.response.PermissionTemplateResponse;
import io.attestry.userauth.interfaces.template.dto.response.TenantRoleTemplateBindingResponse;
import io.attestry.userauth.security.CurrentActor;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAuthority('SCOPE_TENANT_ROLE_ASSIGN')")
@RequestMapping("/admin/tenants/{tenantId}")
public class TenantTemplateAdminHttp {

    private final TemplateAdminUseCase templateAdminService;

    public TenantTemplateAdminHttp(TemplateAdminUseCase templateAdminService) {
        this.templateAdminService = templateAdminService;
    }

    @PostMapping("/permission-templates")
    public PermissionTemplateResponse createTenantTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @RequestBody CreatePermissionTemplateRequest request
    ) {
        PermissionTemplateResult result = templateAdminService.createTenantTemplate(
            actor,
            tenantId,
            new TemplateAdminUseCase.CreateTemplateCommand(request.code(), request.name(), request.description())
        );
        return PermissionTemplateResponse.from(result);
    }

    @GetMapping("/permission-templates")
    public List<PermissionTemplateResponse> listTenantTemplates(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId
    ) {
        return templateAdminService.listTenantTemplates(actor, tenantId).stream()
            .map(PermissionTemplateResponse::from)
            .toList();
    }

    @GetMapping("/permission-templates/{templateCode}")
    public PermissionTemplateResponse getTenantTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("templateCode") String templateCode
    ) {
        return PermissionTemplateResponse.from(templateAdminService.getTenantTemplate(actor, tenantId, templateCode));
    }

    @PatchMapping("/permission-templates/{templateCode}")
    public PermissionTemplateResponse updateTenantTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("templateCode") String templateCode,
        @RequestBody UpdatePermissionTemplateRequest request
    ) {
        PermissionTemplateResult result = templateAdminService.updateTenantTemplate(
            actor,
            tenantId,
            templateCode,
            new TemplateAdminUseCase.UpdateTemplateCommand(request.name(), request.description(), request.enabled())
        );
        return PermissionTemplateResponse.from(result);
    }

    @PutMapping("/permission-templates/{templateCode}/permissions")
    public PermissionTemplateResponse replaceTenantTemplatePermissions(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("templateCode") String templateCode,
        @RequestBody SetTemplatePermissionsRequest request
    ) {
        PermissionTemplateResult result = templateAdminService.replaceTenantTemplatePermissions(
            actor,
            tenantId,
            templateCode,
            new TemplateAdminUseCase.SetTemplatePermissionsCommand(request.permissionCodes())
        );
        return PermissionTemplateResponse.from(result);
    }

    @PostMapping("/permission-templates/{templateCode}/permissions")
    public PermissionTemplateResponse addTenantTemplatePermissions(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("templateCode") String templateCode,
        @RequestBody SetTemplatePermissionsRequest request
    ) {
        PermissionTemplateResult result = templateAdminService.addTenantTemplatePermissions(
            actor,
            tenantId,
            templateCode,
            new TemplateAdminUseCase.AddTemplatePermissionsCommand(request.permissionCodes())
        );
        return PermissionTemplateResponse.from(result);
    }

    @DeleteMapping("/permission-templates/{templateCode}/permissions/{permissionCode}")
    public PermissionTemplateResponse removeTenantTemplatePermission(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("templateCode") String templateCode,
        @PathVariable("permissionCode") String permissionCode
    ) {
        return PermissionTemplateResponse.from(
            templateAdminService.removeTenantTemplatePermission(actor, tenantId, templateCode, permissionCode)
        );
    }

    @PostMapping("/role-templates")
    public TenantRoleTemplateBindingResponse bindTenantRoleTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @RequestBody BindTenantRoleTemplateRequest request
    ) {
        TenantRoleTemplateBindingResult result = templateAdminService.bindTenantRoleTemplate(
            actor,
            tenantId,
            new TemplateAdminUseCase.BindTenantRoleTemplateCommand(request.roleCode(), request.templateCode())
        );
        return TenantRoleTemplateBindingResponse.from(result);
    }

    @GetMapping("/role-templates")
    public List<TenantRoleTemplateBindingResponse> listTenantRoleTemplates(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId
    ) {
        return templateAdminService.listTenantRoleTemplateBindings(actor, tenantId).stream()
            .map(TenantRoleTemplateBindingResponse::from)
            .toList();
    }

    @DeleteMapping("/role-templates/{roleCode}/{templateCode}")
    public void unbindTenantRoleTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("roleCode") String roleCode,
        @PathVariable("templateCode") String templateCode
    ) {
        templateAdminService.unbindTenantRoleTemplate(actor, tenantId, roleCode, templateCode);
    }
}

package io.attestry.userauth.interfaces.template;

import io.attestry.commonlib.web.CurrentActor;
import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.result.PermissionResult;
import io.attestry.userauth.application.dto.result.PermissionTemplateResult;
import io.attestry.userauth.application.dto.result.TenantRoleTemplateBindingResult;
import io.attestry.userauth.application.usecase.template.TemplateManagementUseCase;
import io.attestry.userauth.interfaces.template.dto.request.BindTenantRoleTemplateRequest;
import io.attestry.userauth.interfaces.template.dto.request.CreatePermissionTemplateRequest;
import io.attestry.userauth.interfaces.template.dto.request.SetTemplatePermissionsRequest;
import io.attestry.userauth.interfaces.template.dto.request.UpdatePermissionTemplateRequest;
import io.attestry.userauth.interfaces.template.dto.response.PermissionResponse;
import io.attestry.userauth.interfaces.template.dto.response.PermissionTemplateResponse;
import io.attestry.userauth.interfaces.template.dto.response.TenantRoleTemplateBindingResponse;
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
public class TenantTemplateManagementHttp {

    private final TemplateManagementUseCase templateManagementUseCase;

    public TenantTemplateManagementHttp(TemplateManagementUseCase templateManagementUseCase) {
        this.templateManagementUseCase = templateManagementUseCase;
    }

    @PostMapping("/permission-templates")
    public ApiResponse<PermissionTemplateResponse> createTenantTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @RequestBody CreatePermissionTemplateRequest request
    ) {
        PermissionTemplateResult result = templateManagementUseCase.createTenantTemplate(
            actor,
            tenantId,
            new TemplateManagementUseCase.CreateTemplateCommand(request.code(), request.name(), request.description())
        );
        return ApiResponse.success(PermissionTemplateResponse.from(result));
    }

    @GetMapping("/permission-templates")
    public ApiResponse<List<PermissionTemplateResponse>> listTenantTemplates(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId
    ) {
        return ApiResponse.success(templateManagementUseCase.listTenantTemplates(actor, tenantId).stream()
            .map(PermissionTemplateResponse::from)
            .toList());
    }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionResponse>> listPermissions(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId
    ) {
        return ApiResponse.success(templateManagementUseCase.listPermissions(actor).stream()
            .map(PermissionResponse::from)
            .toList());
    }

    @GetMapping("/permission-templates/{templateCode}")
    public ApiResponse<PermissionTemplateResponse> getTenantTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("templateCode") String templateCode
    ) {
        return ApiResponse.success(PermissionTemplateResponse.from(templateManagementUseCase.getTenantTemplate(actor, tenantId, templateCode)));
    }

    @PatchMapping("/permission-templates/{templateCode}")
    public ApiResponse<PermissionTemplateResponse> updateTenantTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("templateCode") String templateCode,
        @RequestBody UpdatePermissionTemplateRequest request
    ) {
        PermissionTemplateResult result = templateManagementUseCase.updateTenantTemplate(
            actor,
            tenantId,
            templateCode,
            new TemplateManagementUseCase.UpdateTemplateCommand(request.name(), request.description(), request.enabled())
        );
        return ApiResponse.success(PermissionTemplateResponse.from(result));
    }

    @PutMapping("/permission-templates/{templateCode}/permissions")
    public ApiResponse<PermissionTemplateResponse> replaceTenantTemplatePermissions(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("templateCode") String templateCode,
        @RequestBody SetTemplatePermissionsRequest request
    ) {
        PermissionTemplateResult result = templateManagementUseCase.replaceTenantTemplatePermissions(
            actor,
            tenantId,
            templateCode,
            new TemplateManagementUseCase.SetTemplatePermissionsCommand(request.permissionCodes())
        );
        return ApiResponse.success(PermissionTemplateResponse.from(result));
    }

    @PostMapping("/permission-templates/{templateCode}/permissions")
    public ApiResponse<PermissionTemplateResponse> addTenantTemplatePermissions(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("templateCode") String templateCode,
        @RequestBody SetTemplatePermissionsRequest request
    ) {
        PermissionTemplateResult result = templateManagementUseCase.addTenantTemplatePermissions(
            actor,
            tenantId,
            templateCode,
            new TemplateManagementUseCase.AddTemplatePermissionsCommand(request.permissionCodes())
        );
        return ApiResponse.success(PermissionTemplateResponse.from(result));
    }

    @DeleteMapping("/permission-templates/{templateCode}/permissions/{permissionCode}")
    public ApiResponse<PermissionTemplateResponse> removeTenantTemplatePermission(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("templateCode") String templateCode,
        @PathVariable("permissionCode") String permissionCode
    ) {
        return ApiResponse.success(PermissionTemplateResponse.from(
            templateManagementUseCase.removeTenantTemplatePermission(actor, tenantId, templateCode, permissionCode)
        ));
    }

    @PostMapping("/role-templates")
    public ApiResponse<TenantRoleTemplateBindingResponse> bindTenantRoleTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @RequestBody BindTenantRoleTemplateRequest request
    ) {
        TenantRoleTemplateBindingResult result = templateManagementUseCase.bindTenantRoleTemplate(
            actor,
            tenantId,
            new TemplateManagementUseCase.BindTenantRoleTemplateCommand(request.roleCode(), request.templateCode())
        );
        return ApiResponse.success(TenantRoleTemplateBindingResponse.from(result));
    }

    @GetMapping("/role-templates")
    public ApiResponse<List<TenantRoleTemplateBindingResponse>> listTenantRoleTemplates(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId
    ) {
        return ApiResponse.success(templateManagementUseCase.listTenantRoleTemplateBindings(actor, tenantId).stream()
            .map(TenantRoleTemplateBindingResponse::from)
            .toList());
    }

    @DeleteMapping("/role-templates/{roleCode}/{templateCode}")
    public void unbindTenantRoleTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("roleCode") String roleCode,
        @PathVariable("templateCode") String templateCode
    ) {
        templateManagementUseCase.unbindTenantRoleTemplate(actor, tenantId, roleCode, templateCode);
    }
}

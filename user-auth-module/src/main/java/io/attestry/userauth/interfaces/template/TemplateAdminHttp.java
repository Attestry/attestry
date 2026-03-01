package io.attestry.userauth.interfaces.template;

import io.attestry.userauth.application.dto.result.PermissionTemplateResult;
import io.attestry.userauth.application.dto.result.TenantRoleTemplateBindingResult;
import io.attestry.userauth.application.usecase.membership.TemplateAdminUseCase;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.interfaces.template.dto.request.BindTenantRoleTemplateRequest;
import io.attestry.userauth.interfaces.template.dto.request.CreatePermissionTemplateRequest;
import io.attestry.userauth.interfaces.template.dto.request.SetTemplatePermissionsRequest;
import io.attestry.userauth.interfaces.template.dto.request.UpdatePermissionTemplateRequest;
import io.attestry.userauth.interfaces.template.dto.response.PermissionTemplateResponse;
import io.attestry.userauth.interfaces.template.dto.response.TenantRoleTemplateBindingResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('SCOPE_PLATFORM_ADMIN')")
public class TemplateAdminHttp {

    private final TemplateAdminUseCase templateAdminService;

    public TemplateAdminHttp(TemplateAdminUseCase templateAdminService) {
        this.templateAdminService = templateAdminService;
    }

    @PostMapping("/permission-templates")
    public PermissionTemplateResponse createTemplate(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody CreatePermissionTemplateRequest request
    ) {
        PermissionTemplateResult result = templateAdminService.createTemplate(
            principal,
            new TemplateAdminUseCase.CreateTemplateCommand(request.code(), request.name(), request.description())
        );
        return PermissionTemplateResponse.from(result);
    }

    @GetMapping("/permission-templates")
    public List<PermissionTemplateResponse> listTemplates(@AuthenticationPrincipal AuthPrincipal principal) {
        return templateAdminService.listTemplates(principal).stream()
            .map(PermissionTemplateResponse::from)
            .toList();
    }

    @GetMapping("/permission-templates/{templateCode}")
    public PermissionTemplateResponse getTemplate(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("templateCode") String templateCode
    ) {
        return PermissionTemplateResponse.from(templateAdminService.getTemplate(principal, templateCode));
    }

    @PatchMapping("/permission-templates/{templateCode}")
    public PermissionTemplateResponse updateTemplate(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("templateCode") String templateCode,
        @RequestBody UpdatePermissionTemplateRequest request
    ) {
        PermissionTemplateResult result = templateAdminService.updateTemplate(
            principal,
            templateCode,
            new TemplateAdminUseCase.UpdateTemplateCommand(request.name(), request.description(), request.enabled())
        );
        return PermissionTemplateResponse.from(result);
    }

    @PutMapping("/permission-templates/{templateCode}/permissions")
    public PermissionTemplateResponse replaceTemplatePermissions(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("templateCode") String templateCode,
        @RequestBody SetTemplatePermissionsRequest request
    ) {
        PermissionTemplateResult result = templateAdminService.replaceTemplatePermissions(
            principal,
            templateCode,
            new TemplateAdminUseCase.SetTemplatePermissionsCommand(request.permissionCodes())
        );
        return PermissionTemplateResponse.from(result);
    }

    @PostMapping("/permission-templates/{templateCode}/permissions")
    public PermissionTemplateResponse addTemplatePermissions(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("templateCode") String templateCode,
        @RequestBody SetTemplatePermissionsRequest request
    ) {
        PermissionTemplateResult result = templateAdminService.addTemplatePermissions(
            principal,
            templateCode,
            new TemplateAdminUseCase.AddTemplatePermissionsCommand(request.permissionCodes())
        );
        return PermissionTemplateResponse.from(result);
    }

    @DeleteMapping("/permission-templates/{templateCode}/permissions/{permissionCode}")
    public PermissionTemplateResponse removeTemplatePermission(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("templateCode") String templateCode,
        @PathVariable("permissionCode") String permissionCode
    ) {
        return PermissionTemplateResponse.from(
            templateAdminService.removeTemplatePermission(principal, templateCode, permissionCode)
        );
    }

    @PostMapping("/tenants/{tenantId}/role-templates")
    public TenantRoleTemplateBindingResponse bindTenantRoleTemplate(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @RequestBody BindTenantRoleTemplateRequest request
    ) {
        TenantRoleTemplateBindingResult result = templateAdminService.bindTenantRoleTemplate(
            principal,
            tenantId,
            new TemplateAdminUseCase.BindTenantRoleTemplateCommand(request.roleCode(), request.templateCode())
        );
        return TenantRoleTemplateBindingResponse.from(result);
    }

    @GetMapping("/tenants/{tenantId}/role-templates")
    public List<TenantRoleTemplateBindingResponse> listTenantRoleTemplates(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId
    ) {
        return templateAdminService.listTenantRoleTemplateBindings(principal, tenantId).stream()
            .map(TenantRoleTemplateBindingResponse::from)
            .toList();
    }

    @DeleteMapping("/tenants/{tenantId}/role-templates/{roleCode}/{templateCode}")
    public void unbindTenantRoleTemplate(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("tenantId") String tenantId,
        @PathVariable("roleCode") String roleCode,
        @PathVariable("templateCode") String templateCode
    ) {
        templateAdminService.unbindTenantRoleTemplate(principal, tenantId, roleCode, templateCode);
    }
}

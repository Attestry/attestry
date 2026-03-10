package io.attestry.userauth.interfaces.template;

import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.result.PermissionResult;
import io.attestry.userauth.application.dto.result.PermissionTemplateResult;
import io.attestry.userauth.application.usecase.template.TemplateManagementUseCase;
import io.attestry.userauth.interfaces.template.dto.request.CreatePermissionRequest;
import io.attestry.userauth.interfaces.template.dto.request.CreatePermissionTemplateRequest;
import io.attestry.userauth.interfaces.template.dto.request.SetTemplatePermissionsRequest;
import io.attestry.userauth.interfaces.template.dto.request.UpdatePermissionTemplateRequest;
import io.attestry.userauth.interfaces.template.dto.response.PermissionResponse;
import io.attestry.userauth.interfaces.template.dto.response.PermissionTemplateResponse;
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
@PreAuthorize("hasAuthority('SCOPE_PLATFORM_ADMIN')")
@RequestMapping("/admin/permission-templates")
public class PlatformTemplateManagementHttp {

    private final TemplateManagementUseCase templateManagementUseCase;

    public PlatformTemplateManagementHttp(TemplateManagementUseCase templateManagementUseCase) {
        this.templateManagementUseCase = templateManagementUseCase;
    }

    @PostMapping("/permissions")
    public ApiResponse<PermissionResponse> createPermission(
        @CurrentActor ActorContext actor,
        @RequestBody CreatePermissionRequest request
    ) {
        PermissionResult result = templateManagementUseCase.createPermission(
            actor,
            new TemplateManagementUseCase.CreatePermissionCommand(
                request.code(),
                request.name(),
                request.description(),
                request.resourceType(),
                request.action()
            )
        );
        return ApiResponse.success(PermissionResponse.from(result));
    }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionResponse>> listPermissions(@CurrentActor ActorContext actor) {
        return ApiResponse.success(templateManagementUseCase.listPermissions(actor).stream()
            .map(PermissionResponse::from)
            .toList());
    }

    @PostMapping
    public ApiResponse<PermissionTemplateResponse> createTemplate(
        @CurrentActor ActorContext actor,
        @RequestBody CreatePermissionTemplateRequest request
    ) {
        PermissionTemplateResult result = templateManagementUseCase.createTemplate(
            actor,
            new TemplateManagementUseCase.CreateTemplateCommand(request.code(), request.name(), request.description())
        );
        return ApiResponse.success(PermissionTemplateResponse.from(result));
    }

    @GetMapping
    public ApiResponse<List<PermissionTemplateResponse>> listTemplates(@CurrentActor ActorContext actor) {
        return ApiResponse.success(templateManagementUseCase.listTemplates(actor).stream()
            .map(PermissionTemplateResponse::from)
            .toList());
    }

    @GetMapping("/{templateCode}")
    public ApiResponse<PermissionTemplateResponse> getTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("templateCode") String templateCode
    ) {
        return ApiResponse.success(PermissionTemplateResponse.from(templateManagementUseCase.getTemplate(actor, templateCode)));
    }

    @PatchMapping("/{templateCode}")
    public ApiResponse<PermissionTemplateResponse> updateTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("templateCode") String templateCode,
        @RequestBody UpdatePermissionTemplateRequest request
    ) {
        PermissionTemplateResult result = templateManagementUseCase.updateTemplate(
            actor,
            templateCode,
            new TemplateManagementUseCase.UpdateTemplateCommand(request.name(), request.description(), request.enabled())
        );
        return ApiResponse.success(PermissionTemplateResponse.from(result));
    }

    @PutMapping("/{templateCode}/permissions")
    public ApiResponse<PermissionTemplateResponse> replaceTemplatePermissions(
        @CurrentActor ActorContext actor,
        @PathVariable("templateCode") String templateCode,
        @RequestBody SetTemplatePermissionsRequest request
    ) {
        PermissionTemplateResult result = templateManagementUseCase.replaceTemplatePermissions(
            actor,
            templateCode,
            new TemplateManagementUseCase.SetTemplatePermissionsCommand(request.permissionCodes())
        );
        return ApiResponse.success(PermissionTemplateResponse.from(result));
    }

    @PostMapping("/{templateCode}/permissions")
    public ApiResponse<PermissionTemplateResponse> addTemplatePermissions(
        @CurrentActor ActorContext actor,
        @PathVariable("templateCode") String templateCode,
        @RequestBody SetTemplatePermissionsRequest request
    ) {
        PermissionTemplateResult result = templateManagementUseCase.addTemplatePermissions(
            actor,
            templateCode,
            new TemplateManagementUseCase.AddTemplatePermissionsCommand(request.permissionCodes())
        );
        return ApiResponse.success(PermissionTemplateResponse.from(result));
    }

    @DeleteMapping("/{templateCode}/permissions/{permissionCode}")
    public ApiResponse<PermissionTemplateResponse> removeTemplatePermission(
        @CurrentActor ActorContext actor,
        @PathVariable("templateCode") String templateCode,
        @PathVariable("permissionCode") String permissionCode
    ) {
        return ApiResponse.success(PermissionTemplateResponse.from(
            templateManagementUseCase.removeTemplatePermission(actor, templateCode, permissionCode)
        ));
    }
}

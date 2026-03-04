package io.attestry.userauth.interfaces.template;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.result.PermissionResult;
import io.attestry.userauth.application.dto.result.PermissionTemplateResult;
import io.attestry.userauth.application.usecase.membership.TemplateAdminUseCase;
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
public class PlatformTemplateAdminHttp {

    private final TemplateAdminUseCase templateAdminService;

    public PlatformTemplateAdminHttp(TemplateAdminUseCase templateAdminService) {
        this.templateAdminService = templateAdminService;
    }

    @PostMapping("/permissions")
    public PermissionResponse createPermission(
        @CurrentActor ActorContext actor,
        @RequestBody CreatePermissionRequest request
    ) {
        PermissionResult result = templateAdminService.createPermission(
            actor,
            new TemplateAdminUseCase.CreatePermissionCommand(
                request.code(),
                request.name(),
                request.description(),
                request.resourceType(),
                request.action()
            )
        );
        return PermissionResponse.from(result);
    }

    @GetMapping("/permissions")
    public List<PermissionResponse> listPermissions(@CurrentActor ActorContext actor) {
        return templateAdminService.listPermissions(actor).stream()
            .map(PermissionResponse::from)
            .toList();
    }

    @PostMapping
    public PermissionTemplateResponse createTemplate(
        @CurrentActor ActorContext actor,
        @RequestBody CreatePermissionTemplateRequest request
    ) {
        PermissionTemplateResult result = templateAdminService.createTemplate(
            actor,
            new TemplateAdminUseCase.CreateTemplateCommand(request.code(), request.name(), request.description())
        );
        return PermissionTemplateResponse.from(result);
    }

    @GetMapping
    public List<PermissionTemplateResponse> listTemplates(@CurrentActor ActorContext actor) {
        return templateAdminService.listTemplates(actor).stream()
            .map(PermissionTemplateResponse::from)
            .toList();
    }

    @GetMapping("/{templateCode}")
    public PermissionTemplateResponse getTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("templateCode") String templateCode
    ) {
        return PermissionTemplateResponse.from(templateAdminService.getTemplate(actor, templateCode));
    }

    @PatchMapping("/{templateCode}")
    public PermissionTemplateResponse updateTemplate(
        @CurrentActor ActorContext actor,
        @PathVariable("templateCode") String templateCode,
        @RequestBody UpdatePermissionTemplateRequest request
    ) {
        PermissionTemplateResult result = templateAdminService.updateTemplate(
            actor,
            templateCode,
            new TemplateAdminUseCase.UpdateTemplateCommand(request.name(), request.description(), request.enabled())
        );
        return PermissionTemplateResponse.from(result);
    }

    @PutMapping("/{templateCode}/permissions")
    public PermissionTemplateResponse replaceTemplatePermissions(
        @CurrentActor ActorContext actor,
        @PathVariable("templateCode") String templateCode,
        @RequestBody SetTemplatePermissionsRequest request
    ) {
        PermissionTemplateResult result = templateAdminService.replaceTemplatePermissions(
            actor,
            templateCode,
            new TemplateAdminUseCase.SetTemplatePermissionsCommand(request.permissionCodes())
        );
        return PermissionTemplateResponse.from(result);
    }

    @PostMapping("/{templateCode}/permissions")
    public PermissionTemplateResponse addTemplatePermissions(
        @CurrentActor ActorContext actor,
        @PathVariable("templateCode") String templateCode,
        @RequestBody SetTemplatePermissionsRequest request
    ) {
        PermissionTemplateResult result = templateAdminService.addTemplatePermissions(
            actor,
            templateCode,
            new TemplateAdminUseCase.AddTemplatePermissionsCommand(request.permissionCodes())
        );
        return PermissionTemplateResponse.from(result);
    }

    @DeleteMapping("/{templateCode}/permissions/{permissionCode}")
    public PermissionTemplateResponse removeTemplatePermission(
        @CurrentActor ActorContext actor,
        @PathVariable("templateCode") String templateCode,
        @PathVariable("permissionCode") String permissionCode
    ) {
        return PermissionTemplateResponse.from(
            templateAdminService.removeTemplatePermission(actor, templateCode, permissionCode)
        );
    }
}

package io.attestry.userauth.interfaces.template.dto.response;

import io.attestry.userauth.application.dto.result.PermissionTemplateResult;
import java.util.List;

public record PermissionTemplateResponse(
    String templateId,
    String code,
    String name,
    String description,
    boolean enabled,
    List<String> permissionCodes
) {
    public static PermissionTemplateResponse from(PermissionTemplateResult result) {
        return new PermissionTemplateResponse(
            result.templateId(),
            result.code(),
            result.name(),
            result.description(),
            result.enabled(),
            result.permissionCodes()
        );
    }
}

package io.attestry.userauth.application.template.result;

import java.util.List;

public record PermissionTemplateResult(
    String templateId,
    String code,
    String name,
    String description,
    boolean enabled,
    List<String> permissionCodes
) {
}

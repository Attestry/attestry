package io.attestry.userauth.interfaces.template.dto.request;

public record CreatePermissionTemplateRequest(
    String code,
    String name,
    String description
) {
}

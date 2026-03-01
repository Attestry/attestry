package io.attestry.userauth.interfaces.template.dto.request;

public record UpdatePermissionTemplateRequest(
    String name,
    String description,
    Boolean enabled
) {
}

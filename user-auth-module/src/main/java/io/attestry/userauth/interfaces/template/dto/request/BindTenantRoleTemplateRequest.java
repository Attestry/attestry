package io.attestry.userauth.interfaces.template.dto.request;

public record BindTenantRoleTemplateRequest(
    String roleCode,
    String templateCode
) {
}

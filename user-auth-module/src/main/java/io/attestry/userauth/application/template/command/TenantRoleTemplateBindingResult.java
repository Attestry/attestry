package io.attestry.userauth.application.template.command;

public record TenantRoleTemplateBindingResult(
    String bindingId,
    String tenantId,
    String roleCode,
    String templateCode,
    boolean enabled
) {
}

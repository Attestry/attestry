package io.attestry.userauth.application.template.result;

public record TenantRoleTemplateBindingResult(
    String bindingId,
    String tenantId,
    String roleCode,
    String templateCode,
    boolean enabled
) {
}

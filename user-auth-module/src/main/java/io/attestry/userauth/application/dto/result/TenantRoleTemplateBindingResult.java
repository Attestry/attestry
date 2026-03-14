package io.attestry.userauth.application.dto.result;

public record TenantRoleTemplateBindingResult(
    String bindingId,
    String tenantId,
    String roleCode,
    String templateCode,
    boolean enabled
) {
}

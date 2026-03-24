package io.attestry.userauth.interfaces.template.dto.response;

import io.attestry.userauth.application.template.result.TenantRoleTemplateBindingResult;

public record TenantRoleTemplateBindingResponse(
    String bindingId,
    String tenantId,
    String roleCode,
    String templateCode,
    boolean enabled
) {
    public static TenantRoleTemplateBindingResponse from(TenantRoleTemplateBindingResult result) {
        return new TenantRoleTemplateBindingResponse(
            result.bindingId(),
            result.tenantId(),
            result.roleCode(),
            result.templateCode(),
            result.enabled()
        );
    }
}

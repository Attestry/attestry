package io.attestry.userauth.application.port.template;

import java.time.Instant;
import java.util.List;

public interface TenantRoleTemplateBindingPort {

    TenantRoleTemplateBindingView bindTemplateToTenantRole(
            String tenantId,
            String roleCode,
            String templateCode,
            String actorUserId,
            Instant now);

    List<TenantRoleTemplateBindingView> findTenantRoleTemplateBindings(String tenantId);

    void disableTenantRoleTemplateBinding(String tenantId, String roleCode, String templateCode, Instant now);

    record TenantRoleTemplateBindingView(
            String bindingId,
            String tenantId,
            String roleCode,
            String templateCode,
            boolean enabled) {
    }
}

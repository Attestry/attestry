package io.attestry.workflow.application.port;

public interface TenantReadPort {
    boolean existsActiveTenant(String tenantId);
}

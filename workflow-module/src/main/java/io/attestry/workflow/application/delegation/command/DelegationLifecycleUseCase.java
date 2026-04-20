package io.attestry.workflow.application.delegation.command;

public interface DelegationLifecycleUseCase {
    DelegationEvaluateResult evaluate(
        String sourceTenantId,
        String targetTenantId,
        String resourceType,
        String resourceId,
        String permissionCode
    );

    void consumeByPassportId(String passportId);
}

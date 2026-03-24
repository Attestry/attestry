package io.attestry.workflow.application.delegation.usecase;

import io.attestry.workflow.application.delegation.result.DelegationEvaluateResult;

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

package io.attestry.workflow.application.usecase;

import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.delegation.result.DelegationEvaluateResult;
import io.attestry.workflow.application.delegation.result.DelegationResult;
import java.util.List;

public interface DelegationUseCase {
    DelegationResult grant(AuthPrincipal principal, String sourceTenantId, GrantDelegationCommand command);

    DelegationResult revoke(AuthPrincipal principal, String delegationId, String reason);

    List<DelegationResult> listByTenant(AuthPrincipal principal, String tenantId);

    DelegationEvaluateResult evaluate(
        String sourceTenantId,
        String targetTenantId,
        String resourceType,
        String resourceId,
        String permissionCode
    );
}

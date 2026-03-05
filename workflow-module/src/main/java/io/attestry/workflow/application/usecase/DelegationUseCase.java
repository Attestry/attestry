package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.delegation.command.BatchGrantPassportDelegationCommand;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.delegation.result.BatchDelegationResult;
import io.attestry.workflow.application.delegation.result.DelegationResult;
import java.util.List;

public interface DelegationUseCase {
    DelegationResult grant(AuthPrincipal principal, String sourceTenantId, GrantDelegationCommand command);

    BatchDelegationResult batchGrantPassportDelegation(
        AuthPrincipal principal,
        String sourceTenantId,
        String partnerLinkId,
        BatchGrantPassportDelegationCommand command
    );

    DelegationResult revoke(AuthPrincipal principal, String delegationId, String reason);

    List<DelegationResult> listByTenant(AuthPrincipal principal, String tenantId);
}

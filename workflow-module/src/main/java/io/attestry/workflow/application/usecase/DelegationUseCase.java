package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.delegation.result.DelegationResult;

public interface DelegationUseCase {
    DelegationResult grant(AuthPrincipal principal, String sourceTenantId, GrantDelegationCommand command);

    DelegationResult revoke(AuthPrincipal principal, String delegationId, String reason);
}

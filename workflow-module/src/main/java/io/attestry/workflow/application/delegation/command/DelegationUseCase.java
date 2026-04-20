package io.attestry.workflow.application.delegation.command;

import io.attestry.workflow.application.common.WorkflowActorContext;

public interface DelegationUseCase {
    DelegationResult grant(WorkflowActorContext principal, String sourceTenantId, GrantDelegationCommand command);

    DelegationResult revoke(WorkflowActorContext principal, String delegationId, String reason);
}

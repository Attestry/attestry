package io.attestry.workflow.application.delegation.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.delegation.command.GrantDelegationCommand;
import io.attestry.workflow.application.delegation.result.DelegationResult;

public interface DelegationUseCase {
    DelegationResult grant(WorkflowActorContext principal, String sourceTenantId, GrantDelegationCommand command);

    DelegationResult revoke(WorkflowActorContext principal, String delegationId, String reason);
}

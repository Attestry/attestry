package io.attestry.workflow.application.distribution.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.distribution.command.DistributeCommand;
import io.attestry.workflow.application.distribution.command.RecallDistributionCommand;
import io.attestry.workflow.application.distribution.result.BatchDistributeResult;
import io.attestry.workflow.application.distribution.view.DistributionView;

public interface DistributionCommandUseCase {

    BatchDistributeResult distribute(
        WorkflowActorContext principal,
        String sourceTenantId,
        String partnerLinkId,
        DistributeCommand command
    );

    DistributionView recall(WorkflowActorContext principal, String distributionId, RecallDistributionCommand command);
}

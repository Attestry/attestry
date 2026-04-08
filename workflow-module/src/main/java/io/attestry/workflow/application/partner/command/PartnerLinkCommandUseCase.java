package io.attestry.workflow.application.partner.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.partner.command.CreatePartnerLinkCommand;
import io.attestry.workflow.application.partner.result.PartnerLinkResult;

public interface PartnerLinkCommandUseCase {
    PartnerLinkResult create(WorkflowActorContext principal, CreatePartnerLinkCommand command);

    PartnerLinkResult approve(WorkflowActorContext principal, String partnerLinkId);

    PartnerLinkResult reject(WorkflowActorContext principal, String partnerLinkId, String reason);

    PartnerLinkResult suspend(WorkflowActorContext principal, String partnerLinkId);

    PartnerLinkResult resume(WorkflowActorContext principal, String partnerLinkId);

    PartnerLinkResult terminate(WorkflowActorContext principal, String partnerLinkId, String reason);
}

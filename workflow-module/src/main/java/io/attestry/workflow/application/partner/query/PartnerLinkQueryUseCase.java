package io.attestry.workflow.application.partner.query;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.partner.result.PartnerLinkResult;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import java.util.List;

public interface PartnerLinkQueryUseCase {
    List<PartnerLinkResult> listByTenant(WorkflowActorContext principal, PartnerLinkStatus status);
}

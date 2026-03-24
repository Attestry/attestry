package io.attestry.workflow.application.distribution.usecase;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.distribution.view.PagedDistributionCandidateView;
import io.attestry.workflow.application.distribution.view.PagedDistributionView;

public interface DistributionQueryUseCase {

    PagedDistributionView listByTenant(WorkflowActorContext principal, String sourceTenantId, int page, int size, String keyword);

    PagedDistributionCandidateView listDistributionCandidates(
        WorkflowActorContext principal, int page, int size, String keyword
    );
}

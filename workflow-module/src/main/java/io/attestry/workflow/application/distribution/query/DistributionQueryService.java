package io.attestry.workflow.application.distribution.query;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.distribution.assembler.DistributionViewAssembler;
import io.attestry.workflow.application.distribution.usecase.DistributionQueryUseCase;
import io.attestry.workflow.application.distribution.view.PagedDistributionCandidateView;
import io.attestry.workflow.application.distribution.view.PagedDistributionView;
import io.attestry.workflow.application.port.common.TenantReadPort;
import io.attestry.workflow.application.port.distribution.DistributionCandidateQueryPort;
import io.attestry.workflow.application.port.distribution.DistributionCandidateQueryPort.PagedDistributionCandidateResult;
import io.attestry.workflow.application.port.distribution.DistributionQueryPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DistributionQueryService implements DistributionQueryUseCase {

    private final DistributionCandidateQueryPort distributionCandidateQueryPort;
    private final DistributionQueryPort distributionQueryPort;
    private final TenantReadPort tenantReadPort;
    private final DistributionViewAssembler viewAssembler;
    private final WorkflowAuthorizationSupport authorizationSupport;

    @Override
    @Transactional(readOnly = true)
    public PagedDistributionView listByTenant(
        WorkflowActorContext principal,
        String sourceTenantId,
        int page,
        int size,
        String keyword
    ) {
        assertDistributionReadAccess(principal, sourceTenantId, "distribution:list:" + sourceTenantId);
        var result = distributionQueryPort.findBySourceTenantId(sourceTenantId, page, size, keyword);
        return viewAssembler.toPagedDistributionResponse(result, tenantReadPort.findTenantSummariesByIds(
            result.content().stream()
                .map(DistributionQueryPort.DistributionRow::targetTenantId)
                .distinct()
                .toList()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedDistributionCandidateView listDistributionCandidates(
        WorkflowActorContext principal,
        int page,
        int size,
        String keyword
    ) {
        String tenantId = requireTenantId(principal);
        assertDistributionReadAccess(principal, tenantId, "distribution:candidates:" + tenantId);
        PagedDistributionCandidateResult result =
            distributionCandidateQueryPort.findDistributionCandidatesByTenantId(
                tenantId, page, size, keyword
            );
        return viewAssembler.toPagedDistributionCandidateResponse(result);
    }

    private String requireTenantId(WorkflowActorContext principal) {
        if (principal.tenantId() == null || principal.tenantId().isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Tenant-scoped token is required");
        }
        return principal.tenantId();
    }

    private void assertDistributionReadAccess(WorkflowActorContext principal, String tenantId, String resourceRef) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.TENANT_READ_ONLY, resourceRef);
    }
}

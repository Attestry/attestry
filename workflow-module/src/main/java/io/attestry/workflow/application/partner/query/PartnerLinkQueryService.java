package io.attestry.workflow.application.partner.query;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.partner.result.PartnerLinkResult;
import io.attestry.workflow.application.partner.usecase.PartnerLinkQueryUseCase;
import io.attestry.workflow.application.port.common.TenantReadPort;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.repository.PartnerLinkRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PartnerLinkQueryService implements PartnerLinkQueryUseCase {

    private final PartnerLinkRepository repository;
    private final TenantReadPort tenantReadPort;
    private final WorkflowAuthorizationSupport authorizationSupport;

    @Override
    @Transactional(readOnly = true)
    public List<PartnerLinkResult> listByTenant(WorkflowActorContext principal, PartnerLinkStatus status) {
        String tenantId = requireTenantId(principal);
        authorizationSupport.assertTenantContext(principal, tenantId);
        List<PartnerLink> links = repository.findByTenantId(tenantId, status);
        Map<String, TenantReadPort.TenantSummary> sourceSummaries = tenantReadPort.findTenantSummariesByIds(
            links.stream()
                .map(PartnerLink::sourceTenantId)
                .distinct()
                .toList()
        );
        Map<String, TenantReadPort.TenantSummary> targetSummaries = tenantReadPort.findTenantSummariesByIds(
            links.stream()
                .map(PartnerLink::targetTenantId)
                .distinct()
                .toList()
        );
        return links.stream()
            .map(link -> toResult(link, sourceSummaries.get(link.sourceTenantId()), targetSummaries))
            .toList();
    }

    private String requireTenantId(WorkflowActorContext principal) {
        if (principal.tenantId() == null || principal.tenantId().isBlank()) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_REQUEST,
                "Tenant-scoped token is required"
            );
        }
        return principal.tenantId();
    }

    private PartnerLinkResult toResult(
        PartnerLink link,
        TenantReadPort.TenantSummary source,
        Map<String, TenantReadPort.TenantSummary> targetSummaries
    ) {
        TenantReadPort.TenantSummary target = targetSummaries.get(link.targetTenantId());
        return new PartnerLinkResult(
            link.partnerLinkId(),
            link.sourceTenantId(),
            source != null ? source.name() : null,
            source != null ? source.type() : null,
            link.targetTenantId(),
            target != null ? target.name() : null,
            link.partnerType().name(),
            link.status().name(),
            link.reason(),
            link.expiresAt()
        );
    }
}

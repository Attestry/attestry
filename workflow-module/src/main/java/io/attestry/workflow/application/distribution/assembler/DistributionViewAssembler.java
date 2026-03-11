package io.attestry.workflow.application.distribution.assembler;

import io.attestry.workflow.application.port.distribution.DistributionCandidateQueryPort;
import io.attestry.workflow.application.port.distribution.DistributionQueryPort;
import io.attestry.workflow.application.usecase.DistributionUseCase.DistributionCandidateView;
import io.attestry.workflow.application.usecase.DistributionUseCase.DistributionView;
import io.attestry.workflow.application.usecase.DistributionUseCase.PagedDistributionCandidateResponse;
import io.attestry.workflow.application.usecase.DistributionUseCase.PagedDistributionResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DistributionViewAssembler {

    public DistributionView toView(DistributionQueryPort.DistributionRow row) {
        return new DistributionView(
            row.distributionId(),
            row.passportId(),
            row.sourceTenantId(),
            row.targetTenantId(),
            row.targetTenantName(),
            row.targetTenantType(),
            row.partnerLinkId(),
            row.delegationId(),
            row.status(),
            row.serialNumber(),
            row.modelName(),
            row.distributedByUserId(),
            row.distributedAt(),
            row.recalledByUserId(),
            row.recalledAt(),
            row.recallReason()
        );
    }

    public PagedDistributionResponse toPagedDistributionResponse(DistributionQueryPort.PagedDistributionResult result) {
        List<DistributionView> content = result.content().stream()
            .map(this::toView)
            .toList();
        return new PagedDistributionResponse(
            content,
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages()
        );
    }

    public PagedDistributionCandidateResponse toPagedDistributionCandidateResponse(
        DistributionCandidateQueryPort.PagedDistributionCandidateResult result
    ) {
        List<DistributionCandidateView> content = result.content().stream()
            .map(candidate -> new DistributionCandidateView(
                candidate.passportId(),
                candidate.assetId(),
                candidate.serialNumber(),
                candidate.modelId(),
                candidate.modelName(),
                candidate.productionBatch(),
                candidate.factoryCode()
            ))
            .toList();
        return new PagedDistributionCandidateResponse(
            content,
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages()
        );
    }
}

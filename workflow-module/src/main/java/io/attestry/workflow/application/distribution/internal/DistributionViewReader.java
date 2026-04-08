package io.attestry.workflow.application.distribution.internal;

import io.attestry.workflow.application.distribution.view.DistributionView;
import io.attestry.workflow.application.port.common.TenantReadPort;
import io.attestry.workflow.application.port.distribution.DistributionQueryPort;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DistributionViewReader {

    private final DistributionQueryPort distributionQueryPort;
    private final TenantReadPort tenantReadPort;
    private final DistributionViewAssembler viewAssembler;

    public DistributionView resolveById(String distributionId) {
        DistributionQueryPort.DistributionRow row = distributionQueryPort.findById(distributionId)
            .orElseThrow(() -> new WorkflowDomainException(
                WorkflowErrorCode.DISTRIBUTION_NOT_FOUND,
                "Distribution not found: " + distributionId
            ));
        TenantReadPort.TenantSummary targetTenant = tenantReadPort.findTenantSummariesByIds(List.of(row.targetTenantId()))
            .get(row.targetTenantId());
        return viewAssembler.toView(row, targetTenant);
    }
}

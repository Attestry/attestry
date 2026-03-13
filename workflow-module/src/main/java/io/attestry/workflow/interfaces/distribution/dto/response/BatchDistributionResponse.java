package io.attestry.workflow.interfaces.distribution.dto.response;

import io.attestry.workflow.application.usecase.DistributionUseCase.BatchDistributeResult;
import java.util.List;

public record BatchDistributionResponse(
    List<BatchDistributionEntryResponse> results,
    int totalRequested,
    long totalDistributed
) {
    public static BatchDistributionResponse from(BatchDistributeResult result) {
        List<BatchDistributionEntryResponse> entries = result.results().stream()
            .map(e -> new BatchDistributionEntryResponse(
                e.passportId(), e.distributionId(), e.delegationId(), e.status(), e.error()
            ))
            .toList();
        return new BatchDistributionResponse(entries, result.totalRequested(), result.totalDistributed());
    }
}

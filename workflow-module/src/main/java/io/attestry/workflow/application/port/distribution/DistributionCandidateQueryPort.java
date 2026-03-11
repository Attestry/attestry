package io.attestry.workflow.application.port.distribution;

import java.util.List;

public interface DistributionCandidateQueryPort {

    PagedDistributionCandidateResult findDistributionCandidatesByTenantId(
        String tenantId, int page, int size, String keyword
    );

    record DistributionCandidate(
        String passportId,
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        String productionBatch,
        String factoryCode
    ) {
    }

    record PagedDistributionCandidateResult(
        List<DistributionCandidate> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }
}

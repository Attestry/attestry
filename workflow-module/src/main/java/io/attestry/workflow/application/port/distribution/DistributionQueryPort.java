package io.attestry.workflow.application.port.distribution;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DistributionQueryPort {

    PagedDistributionResult findBySourceTenantId(String sourceTenantId, int page, int size, String keyword);

    Optional<DistributionRow> findById(String distributionId);

    Optional<DistributionRow> findLatestByPassportId(String passportId);

    record PagedDistributionResult(
        List<DistributionRow> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }

    record DistributionRow(
        String distributionId,
        String passportId,
        String sourceTenantId,
        String targetTenantId,
        String targetTenantName,
        String targetTenantType,
        String partnerLinkId,
        String delegationId,
        String status,
        String serialNumber,
        String modelName,
        String distributedByUserId,
        Instant distributedAt,
        String recalledByUserId,
        Instant recalledAt,
        String recallReason
    ) {
    }
}

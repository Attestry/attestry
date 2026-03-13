package io.attestry.product.application.port.query;

import java.time.Instant;
import java.util.Optional;

public interface PassportDistributionQueryPort {

    Optional<DistributionRecord> findLatestDistribution(String passportId);

    record DistributionRecord(
        String distributionId,
        String targetTenantId,
        String targetTenantName,
        String targetTenantType,
        String partnerLinkId,
        String status,
        Instant distributedAt
    ) {
    }
}

package io.attestry.product.application.port;

import java.time.Instant;
import java.util.Optional;

public interface PassportDistributionQueryPort {

    Optional<DistributionView> findLatestDistribution(String passportId);

    record DistributionView(
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

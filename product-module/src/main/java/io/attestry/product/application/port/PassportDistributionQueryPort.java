package io.attestry.product.application.port;

import java.time.Instant;
import java.util.Optional;

public interface PassportDistributionQueryPort {

    Optional<DistributionView> findActiveDistribution(String passportId);

    record DistributionView(
        String tenantId,
        String tenantName,
        String tenantType,
        String permissionCode,
        String scope,
        Instant grantedAt
    ) {
    }
}

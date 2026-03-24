package io.attestry.product.application.port.projection;

import java.time.Instant;

public interface ProductDistributionProjectionWritePort {

    void refreshDistributionProjection(DistributionPayload payload, String sourceEventId, Long sourceEventVersion, Instant updatedAt);

    record DistributionPayload(
        String passportId,
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

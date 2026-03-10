package io.attestry.product.application.dto.result;

import java.time.Instant;

public record DistributionDetailResult(
    String distributionId,
    String targetTenantId,
    String targetTenantName,
    String targetTenantType,
    String partnerLinkId,
    String status,
    Instant distributedAt
) {
}

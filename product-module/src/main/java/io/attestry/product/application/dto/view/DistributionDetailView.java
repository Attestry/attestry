package io.attestry.product.application.dto.view;

import java.time.Instant;

public record DistributionDetailView(
    String distributionId,
    String targetTenantId,
    String targetTenantName,
    String targetTenantType,
    String partnerLinkId,
    String status,
    Instant distributedAt
) {
}

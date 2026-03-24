package io.attestry.workflow.application.distribution.view;

import java.time.Instant;

public record DistributionView(
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

package io.attestry.workflow.interfaces.distribution.dto.response;

import io.attestry.workflow.application.usecase.DistributionUseCase.DistributionView;
import java.time.Instant;

public record DistributionResponse(
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
    public static DistributionResponse from(DistributionView view) {
        return new DistributionResponse(
            view.distributionId(),
            view.passportId(),
            view.sourceTenantId(),
            view.targetTenantId(),
            view.targetTenantName(),
            view.targetTenantType(),
            view.partnerLinkId(),
            view.delegationId(),
            view.status(),
            view.serialNumber(),
            view.modelName(),
            view.distributedByUserId(),
            view.distributedAt(),
            view.recalledByUserId(),
            view.recalledAt(),
            view.recallReason()
        );
    }
}

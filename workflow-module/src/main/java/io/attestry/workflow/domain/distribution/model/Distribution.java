package io.attestry.workflow.domain.distribution.model;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Instant;
import java.util.UUID;

public record Distribution(
    String distributionId,
    String passportId,
    String sourceTenantId,
    String targetTenantId,
    String partnerLinkId,
    String delegationId,
    DistributionStatus status,
    String distributedByUserId,
    Instant distributedAt,
    String recalledByUserId,
    Instant recalledAt,
    String recallReason
) {

    public static Distribution create(
        String passportId,
        String sourceTenantId,
        String targetTenantId,
        String partnerLinkId,
        String delegationId,
        String actorUserId,
        Instant now
    ) {
        return new Distribution(
            UUID.randomUUID().toString(),
            passportId,
            sourceTenantId,
            targetTenantId,
            partnerLinkId,
            delegationId,
            DistributionStatus.DISTRIBUTED,
            actorUserId,
            now,
            null,
            null,
            null
        );
    }

    public Distribution recall(String actorUserId, String reason, Instant now) {
        if (status != DistributionStatus.DISTRIBUTED) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Only distributed items can be recalled"
            );
        }
        return new Distribution(
            distributionId,
            passportId,
            sourceTenantId,
            targetTenantId,
            partnerLinkId,
            delegationId,
            DistributionStatus.RECALLED,
            distributedByUserId,
            distributedAt,
            actorUserId,
            now,
            reason
        );
    }
}

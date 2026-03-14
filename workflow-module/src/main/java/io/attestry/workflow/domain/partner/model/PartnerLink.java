package io.attestry.workflow.domain.partner.model;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Instant;
import java.util.UUID;

public record PartnerLink(
    String partnerLinkId,
    String sourceTenantId,
    String targetTenantId,
    PartnerType partnerType,
    PartnerLinkStatus status,
    String createdByUserId,
    Instant createdAt,
    String approvedByUserId,
    Instant approvedAt,
    Instant expiresAt,
    Instant terminatedAt,
    String reason
) {
    public static PartnerLink create(
        String sourceTenantId,
        String targetTenantId,
        PartnerType partnerType,
        String actorUserId,
        Instant expiresAt,
        Instant now
    ) {
        return new PartnerLink(
            UUID.randomUUID().toString(),
            sourceTenantId,
        
            targetTenantId,
            partnerType,
            PartnerLinkStatus.PENDING,
            actorUserId,
            now,
            null,
            null,
            expiresAt,
            null,
            null
        );
    }

    public PartnerLink approve(String approverUserId, Instant now) {
        if (status != PartnerLinkStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.PARTNER_LINK_INVALID_STATE, "Only pending partner link can be approved");
        }
        return new PartnerLink(
            partnerLinkId,
            sourceTenantId,
        
            targetTenantId,
            partnerType,
            PartnerLinkStatus.ACTIVE,
            createdByUserId,
            createdAt,
            approverUserId,
            now,
            expiresAt,
            null,
            null
        );
    }

    public PartnerLink reject(String approverUserId, String rejectReason, Instant now) {
        if (status != PartnerLinkStatus.PENDING) {
            throw new WorkflowDomainException(WorkflowErrorCode.PARTNER_LINK_INVALID_STATE, "Only pending partner link can be rejected");
        }
        return new PartnerLink(
            partnerLinkId,
            sourceTenantId,
        
            targetTenantId,
            partnerType,
            PartnerLinkStatus.REJECTED,
            createdByUserId,
            createdAt,
            approverUserId,
            now,
            expiresAt,
            now,
            rejectReason
        );
    }

    public PartnerLink suspend(String actorUserId, Instant now) {
        if (status != PartnerLinkStatus.ACTIVE) {
            throw new WorkflowDomainException(WorkflowErrorCode.PARTNER_LINK_INVALID_STATE, "Only active partner link can be suspended");
        }
        return new PartnerLink(
            partnerLinkId,
            sourceTenantId,
        
            targetTenantId,
            partnerType,
            PartnerLinkStatus.SUSPENDED,
            createdByUserId,
            createdAt,
            actorUserId,
            now,
            expiresAt,
            null,
            null
        );
    }

    public PartnerLink resume(String actorUserId, Instant now) {
        if (status != PartnerLinkStatus.SUSPENDED) {
            throw new WorkflowDomainException(WorkflowErrorCode.PARTNER_LINK_INVALID_STATE, "Only suspended partner link can be resumed");
        }
        return new PartnerLink(
            partnerLinkId,
            sourceTenantId,
        
            targetTenantId,
            partnerType,
            PartnerLinkStatus.ACTIVE,
            createdByUserId,
            createdAt,
            actorUserId,
            now,
            expiresAt,
            null,
            null
        );
    }

    public PartnerLink terminate(String actorUserId, String terminateReason, Instant now) {
        if (status == PartnerLinkStatus.TERMINATED || status == PartnerLinkStatus.REJECTED) {
            throw new WorkflowDomainException(WorkflowErrorCode.PARTNER_LINK_INVALID_STATE, "Already closed partner link");
        }
        return new PartnerLink(
            partnerLinkId,
            sourceTenantId,
        
            targetTenantId,
            partnerType,
            PartnerLinkStatus.TERMINATED,
            createdByUserId,
            createdAt,
            actorUserId,
            now,
            expiresAt,
            now,
            terminateReason
        );
    }

}

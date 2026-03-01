package io.attestry.workflow.domain.partner.model;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;

public record PartnerLink(
    String partnerLinkId,
    String brandTenantId,
    String partnerTenantId,
    PartnerType partnerType,
    PartnerLinkStatus status,
    String createdByUserId,
    Instant createdAt,
    String approvedByUserId,
    Instant approvedAt,
    Instant terminatedAt,
    String reason
) {
    public static PartnerLink create(
        String brandTenantId,
        String partnerTenantId,
        PartnerType partnerType,
        String actorUserId,
        Instant now
    ) {
        return new PartnerLink(
            UUID.randomUUID().toString(),
            brandTenantId,
            partnerTenantId,
            partnerType,
            PartnerLinkStatus.PENDING,
            actorUserId,
            now,
            null,
            null,
            null,
            null
        );
    }

    public PartnerLink approve(String approverUserId, Instant now) {
        if (status != PartnerLinkStatus.PENDING) {
            throw new DomainException(ErrorCode.PARTNER_LINK_INVALID_STATE, "Only pending partner link can be approved");
        }
        return new PartnerLink(
            partnerLinkId,
            brandTenantId,
            partnerTenantId,
            partnerType,
            PartnerLinkStatus.ACTIVE,
            createdByUserId,
            createdAt,
            approverUserId,
            now,
            null,
            null
        );
    }

    public PartnerLink reject(String approverUserId, String rejectReason, Instant now) {
        if (status != PartnerLinkStatus.PENDING) {
            throw new DomainException(ErrorCode.PARTNER_LINK_INVALID_STATE, "Only pending partner link can be rejected");
        }
        return new PartnerLink(
            partnerLinkId,
            brandTenantId,
            partnerTenantId,
            partnerType,
            PartnerLinkStatus.REJECTED,
            createdByUserId,
            createdAt,
            approverUserId,
            now,
            now,
            rejectReason
        );
    }

    public PartnerLink suspend(String actorUserId, Instant now) {
        if (status != PartnerLinkStatus.ACTIVE) {
            throw new DomainException(ErrorCode.PARTNER_LINK_INVALID_STATE, "Only active partner link can be suspended");
        }
        return new PartnerLink(
            partnerLinkId,
            brandTenantId,
            partnerTenantId,
            partnerType,
            PartnerLinkStatus.SUSPENDED,
            createdByUserId,
            createdAt,
            actorUserId,
            now,
            null,
            null
        );
    }

    public PartnerLink resume(String actorUserId, Instant now) {
        if (status != PartnerLinkStatus.SUSPENDED) {
            throw new DomainException(ErrorCode.PARTNER_LINK_INVALID_STATE, "Only suspended partner link can be resumed");
        }
        return new PartnerLink(
            partnerLinkId,
            brandTenantId,
            partnerTenantId,
            partnerType,
            PartnerLinkStatus.ACTIVE,
            createdByUserId,
            createdAt,
            actorUserId,
            now,
            null,
            null
        );
    }

    public PartnerLink terminate(String actorUserId, String terminateReason, Instant now) {
        if (status == PartnerLinkStatus.TERMINATED || status == PartnerLinkStatus.REJECTED) {
            throw new DomainException(ErrorCode.PARTNER_LINK_INVALID_STATE, "Already closed partner link");
        }
        return new PartnerLink(
            partnerLinkId,
            brandTenantId,
            partnerTenantId,
            partnerType,
            PartnerLinkStatus.TERMINATED,
            createdByUserId,
            createdAt,
            actorUserId,
            now,
            now,
            terminateReason
        );
    }
}

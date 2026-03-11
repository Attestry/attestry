package io.attestry.workflow.domain.delegation.model;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Instant;
import java.util.UUID;

public record Delegation(
    String delegationId,
    String partnerLinkId,
    String sourceTenantId,
    String targetTenantId,
    String resourceType,
    String resourceId,
    String permissionCode,
    DelegationStatus status,
    Instant expiresAt,
    String grantedByUserId,
    Instant createdAt,
    String revokedByUserId,
    Instant revokedAt,
    String reason
) {
    public static Delegation grant(
        String partnerLinkId,
        String sourceTenantId,
        String targetTenantId,
        String resourceType,
        String resourceId,
        String permissionCode,
        Instant expiresAt,
        String actorUserId,
        Instant now,
        String note
    ) {
        return new Delegation(
            UUID.randomUUID().toString(),
            partnerLinkId,
            sourceTenantId,
            targetTenantId,
            resourceType,
            resourceId,
            permissionCode,
            DelegationStatus.ACTIVE,
            expiresAt,
            actorUserId,
            now,
            null,
            null,
            note
        );
    }

    public Delegation revoke(String actorUserId, String revokeReason, Instant now) {
        if (status != DelegationStatus.ACTIVE) {
            throw new WorkflowDomainException(WorkflowErrorCode.DELEGATION_INVALID_STATE, "Only active delegation can be revoked");
        }
        return new Delegation(
            delegationId,
            partnerLinkId,
            sourceTenantId,
            targetTenantId,
            resourceType,
            resourceId,
            permissionCode,
            DelegationStatus.REVOKED,
            expiresAt,
            grantedByUserId,
            createdAt,
            actorUserId,
            now,
            revokeReason
        );
    }

    public Delegation consume(Instant now) {
        if (status != DelegationStatus.ACTIVE) {
            throw new WorkflowDomainException(WorkflowErrorCode.DELEGATION_INVALID_STATE, "Only active delegation can be consumed");
        }
        return new Delegation(
            delegationId,
            partnerLinkId,
            sourceTenantId,
            targetTenantId,
            resourceType,
            resourceId,
            permissionCode,
            DelegationStatus.CONSUMED,
            expiresAt,
            grantedByUserId,
            createdAt,
            revokedByUserId,
            revokedAt,
            reason
        );
    }

    public Delegation expire(Instant now) {
        if (status != DelegationStatus.ACTIVE) {
            throw new WorkflowDomainException(WorkflowErrorCode.DELEGATION_INVALID_STATE, "Only active delegation can be expired");
        }
        return new Delegation(
            delegationId,
            partnerLinkId,
            sourceTenantId,
            targetTenantId,
            resourceType,
            resourceId,
            permissionCode,
            DelegationStatus.EXPIRED,
            expiresAt,
            grantedByUserId,
            createdAt,
            revokedByUserId,
            now,
            reason
        );
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    public boolean isPassportPermissionGrant() {
        return "PASSPORT".equals(resourceType)
            && "RETAIL_TRANSFER_CREATE".equals(permissionCode);
    }

    // Backward compatibility for older call-sites.
    public String brandTenantId() {
        return sourceTenantId;
    }

    public String partnerTenantId() {
        return targetTenantId;
    }
}

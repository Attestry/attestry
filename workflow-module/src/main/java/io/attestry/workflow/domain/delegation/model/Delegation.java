package io.attestry.workflow.domain.delegation.model;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;

public record Delegation(
    String delegationId,
    String partnerLinkId,
    String brandTenantId,
    String partnerTenantId,
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
        String brandTenantId,
        String partnerTenantId,
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
            brandTenantId,
            partnerTenantId,
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
            throw new DomainException(ErrorCode.DELEGATION_INVALID_STATE, "Only active delegation can be revoked");
        }
        return new Delegation(
            delegationId,
            partnerLinkId,
            brandTenantId,
            partnerTenantId,
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

    public boolean isExpired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }
}

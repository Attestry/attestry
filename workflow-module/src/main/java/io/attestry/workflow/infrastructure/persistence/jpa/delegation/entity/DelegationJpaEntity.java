package io.attestry.workflow.infrastructure.persistence.jpa.delegation.entity;

import io.attestry.workflow.domain.delegation.model.DelegationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "delegations")
public class DelegationJpaEntity {

    @Id
    @Column(name = "delegation_id", nullable = false, length = 36)
    private String delegationId;

    @Column(name = "partner_link_id", nullable = false, length = 36)
    private String partnerLinkId;

    @Column(name = "source_tenant_id", nullable = false, length = 36)
    private String sourceTenantId;

    @Column(name = "target_tenant_id", nullable = false, length = 36)
    private String targetTenantId;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 100)
    private String resourceId;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DelegationStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "granted_by_user_id", nullable = false, length = 36)
    private String grantedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_by_user_id", length = 36)
    private String revokedByUserId;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected DelegationJpaEntity() {
    }

    public DelegationJpaEntity(
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
        String reason,
        long rowVersion
    ) {
        this.delegationId = delegationId;
        this.partnerLinkId = partnerLinkId;
        this.sourceTenantId = sourceTenantId;
        this.targetTenantId = targetTenantId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.permissionCode = permissionCode;
        this.status = status;
        this.expiresAt = expiresAt;
        this.grantedByUserId = grantedByUserId;
        this.createdAt = createdAt;
        this.revokedByUserId = revokedByUserId;
        this.revokedAt = revokedAt;
        this.reason = reason;
        this.rowVersion = rowVersion;
    }

    public String getDelegationId() {
        return delegationId;
    }

    public String getPartnerLinkId() {
        return partnerLinkId;
    }

    public String getSourceTenantId() {
        return sourceTenantId;
    }

    public String getTargetTenantId() {
        return targetTenantId;
    }

    // Backward compatibility for transitional usages.
    public String getBrandTenantId() {
        return sourceTenantId;
    }

    public String getPartnerTenantId() {
        return targetTenantId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public DelegationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getGrantedByUserId() {
        return grantedByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getRevokedByUserId() {
        return revokedByUserId;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getReason() {
        return reason;
    }

    public long getRowVersion() {
        return rowVersion;
    }
}

package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import io.attestry.userauth.domain.approval.model.PermissionStatus;
import io.attestry.userauth.domain.auth.model.Scope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "passport_permissions")
public class PassportPermissionJpaEntity {

    @Id
    @Column(name = "permission_id", nullable = false, length = 36)
    private String permissionId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "passport_id", nullable = false, length = 36)
    private String passportId;

    @Column(name = "retail_group_id", nullable = false, length = 36)
    private String retailGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private Scope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PermissionStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "granted_by", nullable = false, length = 36)
    private String grantedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PassportPermissionJpaEntity() {
    }

    public PassportPermissionJpaEntity(
        String permissionId,
        String tenantId,
        String passportId,
        String retailGroupId,
        Scope scope,
        PermissionStatus status,
        Instant expiresAt,
        String grantedBy,
        Instant createdAt
    ) {
        this.permissionId = permissionId;
        this.tenantId = tenantId;
        this.passportId = passportId;
        this.retailGroupId = retailGroupId;
        this.scope = scope;
        this.status = status;
        this.expiresAt = expiresAt;
        this.grantedBy = grantedBy;
        this.createdAt = createdAt;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getPassportId() {
        return passportId;
    }

    public String getRetailGroupId() {
        return retailGroupId;
    }

    public Scope getScope() {
        return scope;
    }

    public PermissionStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

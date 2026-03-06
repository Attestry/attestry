package io.attestry.product.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "passport_permissions")
public class PassportPermissionJpaEntity {

    @Id
    @Column(name = "permission_id", nullable = false, length = 36)
    private String permissionId;

    @Column(name = "passport_id", nullable = false, length = 36)
    private String passportId;

    @Column(name = "seller_tenant_id", nullable = false, length = 36)
    private String sellerTenantId;

    @Column(name = "scope", nullable = false, length = 50)
    private String scope;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PassportPermissionJpaEntity() {
    }

    public PassportPermissionJpaEntity(
        String permissionId,
        String passportId,
        String sellerTenantId,
        String scope,
        String status,
        Instant expiresAt,
        Instant createdAt
    ) {
        this.permissionId = permissionId;
        this.passportId = passportId;
        this.sellerTenantId = sellerTenantId;
        this.scope = scope;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getPermissionId() { return permissionId; }
    public String getPassportId() { return passportId; }
    public String getSellerTenantId() { return sellerTenantId; }
    public String getScope() { return scope; }
    public String getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}

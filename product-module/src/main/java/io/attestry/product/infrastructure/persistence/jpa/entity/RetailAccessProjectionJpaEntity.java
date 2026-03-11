package io.attestry.product.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "product_retail_access_projection")
@IdClass(RetailAccessProjectionId.class)
public class RetailAccessProjectionJpaEntity {

    @Id
    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Id
    @Column(name = "passport_id", nullable = false, length = 36)
    private String passportId;

    @Id
    @Column(name = "access_source_type", nullable = false, length = 30)
    private String accessSourceType;

    @Id
    @Column(name = "access_source_id", nullable = false, length = 36)
    private String accessSourceId;

    @Column(name = "source_tenant_id", length = 36)
    private String sourceTenantId;

    @Column(name = "permission_id", length = 36)
    private String permissionId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "access_status", nullable = false, length = 30)
    private String accessStatus;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RetailAccessProjectionJpaEntity() {
    }

    public String getTenantId() { return tenantId; }
    public String getPassportId() { return passportId; }
    public String getAccessSourceType() { return accessSourceType; }
    public String getAccessSourceId() { return accessSourceId; }
    public String getSourceTenantId() { return sourceTenantId; }
    public String getPermissionId() { return permissionId; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getAccessStatus() { return accessStatus; }
    public Instant getGrantedAt() { return grantedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

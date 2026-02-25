package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import io.attestry.userauth.domain.organization.model.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenants")
public class TenantJpaEntity {

    @Id
    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "region", nullable = false, length = 10)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantStatus status;

    protected TenantJpaEntity() {
    }

    public TenantJpaEntity(String tenantId, String name, String region, TenantStatus status) {
        this.tenantId = tenantId;
        this.name = name;
        this.region = region;
        this.status = status;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
    }

    public TenantStatus getStatus() {
        return status;
    }
}

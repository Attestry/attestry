package io.attestry.product.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "product_passports")
public class ProductPassportJpaEntity {

    @Id
    @Column(name = "passport_id", nullable = false, length = 36)
    private String passportId;

    @Column(name = "asset_id", nullable = false, unique = true, length = 36)
    private String assetId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "group_id", nullable = false, length = 36)
    private String groupId;

    @Column(name = "qr_public_code", nullable = false, unique = true, length = 120)
    private String qrPublicCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProductPassportJpaEntity() {
    }

    public ProductPassportJpaEntity(
        String passportId,
        String assetId,
        String tenantId,
        String groupId,
        String qrPublicCode,
        Instant createdAt
    ) {
        this.passportId = passportId;
        this.assetId = assetId;
        this.tenantId = tenantId;
        this.groupId = groupId;
        this.qrPublicCode = qrPublicCode;
        this.createdAt = createdAt;
    }

    public String getPassportId() { return passportId; }
    public String getAssetId() { return assetId; }
    public String getTenantId() { return tenantId; }
    public String getGroupId() { return groupId; }
    public String getQrPublicCode() { return qrPublicCode; }
    public Instant getCreatedAt() { return createdAt; }
}

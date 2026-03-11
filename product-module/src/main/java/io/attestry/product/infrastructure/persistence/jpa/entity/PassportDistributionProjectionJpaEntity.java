package io.attestry.product.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "product_passport_distribution_projection")
public class PassportDistributionProjectionJpaEntity {

    @Id
    @Column(name = "passport_id", nullable = false, length = 36)
    private String passportId;

    @Column(name = "distribution_id", nullable = false, unique = true, length = 36)
    private String distributionId;

    @Column(name = "target_tenant_id", nullable = false, length = 36)
    private String targetTenantId;

    @Column(name = "target_tenant_name", nullable = false, length = 255)
    private String targetTenantName;

    @Column(name = "target_tenant_type", nullable = false, length = 50)
    private String targetTenantType;

    @Column(name = "partner_link_id", length = 36)
    private String partnerLinkId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "distributed_at", nullable = false)
    private Instant distributedAt;

    @Column(name = "source_event_id", nullable = false, unique = true, length = 100)
    private String sourceEventId;

    @Column(name = "source_event_version")
    private Long sourceEventVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PassportDistributionProjectionJpaEntity() {
    }

    public String getPassportId() { return passportId; }
    public String getDistributionId() { return distributionId; }
    public String getTargetTenantId() { return targetTenantId; }
    public String getTargetTenantName() { return targetTenantName; }
    public String getTargetTenantType() { return targetTenantType; }
    public String getPartnerLinkId() { return partnerLinkId; }
    public String getStatus() { return status; }
    public Instant getDistributedAt() { return distributedAt; }
}

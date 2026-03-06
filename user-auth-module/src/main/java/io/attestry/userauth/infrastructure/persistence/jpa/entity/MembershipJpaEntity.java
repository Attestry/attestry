package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import io.attestry.userauth.domain.organization.model.TenantType;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.organization.model.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "memberships")
public class MembershipJpaEntity {

    @Id
    @Column(name = "membership_id", nullable = false, length = 36)
    private String membershipId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false)
    private TenantType groupType;

    // Deprecated: membership.role removed from DB schema (v2 cleanup).
    // Keep transient field for temporary domain/backward-compatibility usage.
    @Transient
    private MembershipRole role = MembershipRole.STAFF;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MembershipStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_status", nullable = false)
    private TenantStatus tenantStatus;

    protected MembershipJpaEntity() {
    }

    public MembershipJpaEntity(
        String membershipId,
        String userId,
        String tenantId,
        TenantType groupType,
        MembershipRole role,
        MembershipStatus status,
        TenantStatus tenantStatus
    ) {
        this.membershipId = membershipId;
        this.userId = userId;
        this.tenantId = tenantId;
        this.groupType = groupType;
        this.role = role;
        this.status = status;
        this.tenantStatus = tenantStatus;
    }

    public String getMembershipId() {
        return membershipId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public TenantType getTenantType() {
        return groupType;
    }

    public MembershipRole getRole() {
        return role;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public TenantStatus getTenantStatus() {
        return tenantStatus;
    }
}

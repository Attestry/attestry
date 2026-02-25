package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.organization.model.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "memberships")
public class MembershipJpaEntity {

    @Id
    @Column(name = "membership_id", nullable = false, length = 36)
    private String membershipId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "group_id", nullable = false, length = 36)
    private String groupId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false)
    private GroupType groupType;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MembershipRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MembershipStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_status", nullable = false)
    private GroupStatus groupStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_status", nullable = false)
    private TenantStatus tenantStatus;

    protected MembershipJpaEntity() {
    }

    public MembershipJpaEntity(
        String membershipId,
        String userId,
        String groupId,
        String tenantId,
        GroupType groupType,
        MembershipRole role,
        MembershipStatus status,
        GroupStatus groupStatus,
        TenantStatus tenantStatus
    ) {
        this.membershipId = membershipId;
        this.userId = userId;
        this.groupId = groupId;
        this.tenantId = tenantId;
        this.groupType = groupType;
        this.role = role;
        this.status = status;
        this.groupStatus = groupStatus;
        this.tenantStatus = tenantStatus;
    }

    public String getMembershipId() {
        return membershipId;
    }

    public String getUserId() {
        return userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public GroupType getGroupType() {
        return groupType;
    }

    public MembershipRole getRole() {
        return role;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public GroupStatus getGroupStatus() {
        return groupStatus;
    }

    public TenantStatus getTenantStatus() {
        return tenantStatus;
    }
}

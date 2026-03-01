package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "membership_role_assignments")
public class MembershipRoleAssignmentJpaEntity {

    @Id
    @Column(name = "assignment_id", nullable = false, length = 36)
    private String assignmentId;

    @Column(name = "membership_id", nullable = false, length = 36)
    private String membershipId;

    @Column(name = "role_id", nullable = false, length = 36)
    private String roleId;

    @Column(name = "assigned_by_user_id", length = 36)
    private String assignedByUserId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected MembershipRoleAssignmentJpaEntity() {
    }

    public MembershipRoleAssignmentJpaEntity(
        String assignmentId,
        String membershipId,
        String roleId,
        String assignedByUserId,
        Instant assignedAt
    ) {
        this.assignmentId = assignmentId;
        this.membershipId = membershipId;
        this.roleId = roleId;
        this.assignedByUserId = assignedByUserId;
        this.assignedAt = assignedAt;
    }

    public String getMembershipId() {
        return membershipId;
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public String getRoleId() {
        return roleId;
    }
}

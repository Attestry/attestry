package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "role_assignment_audits")
public class RoleAssignmentAuditJpaEntity {

    @Id
    @Column(name = "audit_id", nullable = false, length = 36)
    private String auditId;

    @Column(name = "actor_user_id", nullable = false, length = 36)
    private String actorUserId;

    @Column(name = "actor_tenant_id", length = 36)
    private String actorTenantId;

    @Column(name = "target_membership_id", nullable = false, length = 36)
    private String targetMembershipId;

    @Column(name = "before_role", length = 30)
    private String beforeRole;

    @Column(name = "after_role", length = 30)
    private String afterRole;

    @Column(name = "decision_source", nullable = false, length = 30)
    private String decisionSource;

    @Column(name = "allowed", nullable = false)
    private boolean allowed;

    @Column(name = "reason_code", length = 100)
    private String reasonCode;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    protected RoleAssignmentAuditJpaEntity() {
    }

    public RoleAssignmentAuditJpaEntity(
        String auditId,
        String actorUserId,
        String actorTenantId,
        String targetMembershipId,
        String beforeRole,
        String afterRole,
        String decisionSource,
        boolean allowed,
        String reasonCode,
        Instant requestedAt,
        Instant decidedAt
    ) {
        this.auditId = auditId;
        this.actorUserId = actorUserId;
        this.actorTenantId = actorTenantId;
        this.targetMembershipId = targetMembershipId;
        this.beforeRole = beforeRole;
        this.afterRole = afterRole;
        this.decisionSource = decisionSource;
        this.allowed = allowed;
        this.reasonCode = reasonCode;
        this.requestedAt = requestedAt;
        this.decidedAt = decidedAt;
    }
}

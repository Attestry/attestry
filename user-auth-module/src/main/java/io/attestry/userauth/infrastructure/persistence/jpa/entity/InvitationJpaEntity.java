package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import io.attestry.userauth.domain.membership.model.InvitationStatus;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "invitations")
public class InvitationJpaEntity {

    @Id
    @Column(name = "invitation_id", nullable = false, length = 36)
    private String invitationId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "invitee_email", nullable = false)
    private String inviteeEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MembershipRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvitationStatus status;

    @Column(name = "invited_by", nullable = false, length = 36)
    private String invitedBy;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    @Column(name = "accepted_by", length = 36)
    private String acceptedBy;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    protected InvitationJpaEntity() {
    }

    public InvitationJpaEntity(
        String invitationId,
        String tenantId,
        String inviteeEmail,
        MembershipRole role,
        InvitationStatus status,
        String invitedBy,
        Instant invitedAt,
        String acceptedBy,
        Instant acceptedAt
    ) {
        this.invitationId = invitationId;
        this.tenantId = tenantId;
        this.inviteeEmail = inviteeEmail;
        this.role = role;
        this.status = status;
        this.invitedBy = invitedBy;
        this.invitedAt = invitedAt;
        this.acceptedBy = acceptedBy;
        this.acceptedAt = acceptedAt;
    }

    public String getInvitationId() {
        return invitationId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getInviteeEmail() {
        return inviteeEmail;
    }

    public MembershipRole getRole() {
        return role;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public String getInvitedBy() {
        return invitedBy;
    }

    public Instant getInvitedAt() {
        return invitedAt;
    }

    public String getAcceptedBy() {
        return acceptedBy;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }
}

package io.attestry.userauth.domain.membership.model;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.identity.model.Email;
import java.time.Instant;
import java.util.UUID;

public class Invitation {

    private final String invitationId;
    private final String tenantId;
    private final String groupId;
    private final Email inviteeEmail;
    private final MembershipRole role;
    private InvitationStatus status;
    private final String invitedBy;
    private final Instant invitedAt;
    private String acceptedBy;
    private Instant acceptedAt;

    private Invitation(String invitationId, String tenantId, String groupId,
                       Email inviteeEmail, MembershipRole role, InvitationStatus status,
                       String invitedBy, Instant invitedAt, String acceptedBy, Instant acceptedAt) {
        this.invitationId = invitationId;
        this.tenantId = tenantId;
        this.groupId = groupId;
        this.inviteeEmail = inviteeEmail;
        this.role = role;
        this.status = status;
        this.invitedBy = invitedBy;
        this.invitedAt = invitedAt;
        this.acceptedBy = acceptedBy;
        this.acceptedAt = acceptedAt;
    }

    public static Invitation issue(String tenantId, String groupId, String email,
                                    MembershipRole role, String inviterUserId, Instant now) {
        return new Invitation(
            UUID.randomUUID().toString(),
            tenantId,
            groupId,
            Email.of(email),
            role,
            InvitationStatus.PENDING,
            inviterUserId,
            now,
            null,
            null
        );
    }

    public static Invitation reconstitute(String invitationId, String tenantId, String groupId,
                                           Email inviteeEmail, MembershipRole role, InvitationStatus status,
                                           String invitedBy, Instant invitedAt, String acceptedBy, Instant acceptedAt) {
        return new Invitation(invitationId, tenantId, groupId, inviteeEmail, role, status,
            invitedBy, invitedAt, acceptedBy, acceptedAt);
    }

    public void accept(String accepterUserId, Email accepterEmail, Instant now) {
        if (status != InvitationStatus.PENDING) {
            throw new DomainException(ErrorCode.INVALID_APPLICATION_STATE, "Invitation is not pending");
        }
        //TODO("소셜로그인 허용할때 정책 적용 / 또는 회사 계정으로만 가입되게?")
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedBy = accepterUserId;
        this.acceptedAt = now;
    }

    public void revoke() {
        if (status != InvitationStatus.PENDING) {
            throw new DomainException(ErrorCode.INVALID_APPLICATION_STATE, "Invitation is not pending");
        }
        this.status = InvitationStatus.REVOKED;
    }

    // Getters
    public String invitationId() { return invitationId; }
    public String tenantId() { return tenantId; }
    public String groupId() { return groupId; }
    public Email inviteeEmail() { return inviteeEmail; }
    public MembershipRole role() { return role; }
    public InvitationStatus status() { return status; }
    public String invitedBy() { return invitedBy; }
    public Instant invitedAt() { return invitedAt; }
    public String acceptedBy() { return acceptedBy; }
    public Instant acceptedAt() { return acceptedAt; }
}

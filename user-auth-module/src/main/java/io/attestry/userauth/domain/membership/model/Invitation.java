package io.attestry.userauth.domain.membership.model;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.user.vo.Email;
import java.time.Instant;
import java.util.UUID;

public record Invitation(
    String invitationId,
    String tenantId,
    String groupId,
    Email inviteeEmail,
    MembershipRole role,
    InvitationStatus status,
    String invitedBy,
    Instant invitedAt,
    String acceptedBy,
    Instant acceptedAt
) {
    public static Invitation issue(String tenantId, String groupId, String email, MembershipRole role, String inviterUserId, Instant now) {
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

    public Invitation accept(String accepterUserId, Email accepterEmail, Instant now) {
        if (status != InvitationStatus.PENDING) {
            throw new DomainException(ErrorCode.INVALID_APPLICATION_STATE, "Invitation is not pending");
        }
        if (!inviteeEmail.equals(accepterEmail)) {
            throw new DomainException(ErrorCode.FORBIDDEN_SCOPE, "Invitation recipient mismatch");
        }
        return new Invitation(
            invitationId,
            tenantId,
            groupId,
            inviteeEmail,
            role,
            InvitationStatus.ACCEPTED,
            invitedBy,
            invitedAt,
            accepterUserId,
            now
        );
    }
}

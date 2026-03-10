package io.attestry.userauth.domain.membership.model;

public record InvitationNotificationPayload(
    String invitationId,
    String tenantId,
    String inviteeEmail
) {
}

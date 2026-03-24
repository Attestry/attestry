package io.attestry.userauth.application.membership.result;

public record InvitationResult(
    String invitationId,
    String tenantId,
    String inviteeEmail,
    String role,
    String status
) {
}

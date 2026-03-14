package io.attestry.userauth.application.dto.result;

public record InvitationResult(
    String invitationId,
    String tenantId,
    String inviteeEmail,
    String role,
    String status
) {
}

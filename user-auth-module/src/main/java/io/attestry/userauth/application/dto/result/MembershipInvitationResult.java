package io.attestry.userauth.application.dto.result;

public record MembershipInvitationResult(
    String invitationId,
    String tenantId,
    String groupId,
    String inviteeEmail,
    String role,
    String status
) {
}

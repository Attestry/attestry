package io.attestry.userauth.interfaces.membership.dto.response;

import io.attestry.userauth.application.dto.result.InvitationResult;

public record InvitationResponse(
        String invitationId,
        String tenantId,
        String inviteeEmail,
        String role,
        String status
) {
    public static InvitationResponse from(InvitationResult invitation) {
        return new InvitationResponse(
                invitation.invitationId(),
                invitation.tenantId(),
                invitation.inviteeEmail(),
                invitation.role(),
                invitation.status()
        );
    }
}

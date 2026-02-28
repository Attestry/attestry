package io.attestry.userauth.interfaces.membership.dto.response;

import io.attestry.userauth.application.dto.result.MembershipInvitationResult;

public record InvitationResponse(
        String invitationId,
        String tenantId,
        String groupId,
        String inviteeEmail,
        String role,
        String status
) {
    public static InvitationResponse from(MembershipInvitationResult invitation) {
        return new InvitationResponse(
                invitation.invitationId(),
                invitation.tenantId(),
                invitation.groupId(),
                invitation.inviteeEmail(),
                invitation.role(),
                invitation.status()
        );
    }
}

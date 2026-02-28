package io.attestry.userauth.application.usecase.membership;

import io.attestry.userauth.application.dto.result.MembershipInvitationResult;
import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.dto.result.GroupAdminResult;
import io.attestry.userauth.application.dto.view.MembershipAdminView;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import java.util.List;

public interface MembershipAdminUseCase {
    MembershipInvitationResult invite(AuthPrincipal principal, String tenantId, InviteCommand command);

    MembershipResult acceptInvitation(AuthPrincipal principal, String invitationId);

    List<MembershipAdminView> listMemberships(AuthPrincipal principal, String tenantId);

    MembershipResult updateMembershipRole(AuthPrincipal principal, String tenantId, String membershipId, UpdateMembershipRoleCommand command);

    MembershipResult updateMembershipStatus(
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        UpdateMembershipStatusCommand command
    );

    GroupAdminResult suspendGroup(AuthPrincipal principal, String tenantId, String groupId);

    GroupAdminResult unsuspendGroup(AuthPrincipal principal, String tenantId, String groupId);

    record InviteCommand(String email, String groupId, MembershipRole role) {
    }

    record UpdateMembershipRoleCommand(MembershipRole role) {
    }

    record UpdateMembershipStatusCommand(MembershipStatus status) {
    }
}

package io.attestry.userauth.application.usecase.membership;

import io.attestry.userauth.application.dto.result.MembershipInvitationResult;
import io.attestry.userauth.application.dto.result.GroupAdminResult;
import io.attestry.userauth.application.dto.result.MembershipPermissionTemplateResult;
import io.attestry.userauth.application.dto.result.MembershipAssignableRolesResult;
import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;
import io.attestry.userauth.application.dto.view.MembershipAdminView;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import java.util.List;

public interface MembershipAdminUseCase {
    MembershipInvitationResult invite(AuthPrincipal principal, String tenantId, InviteCommand command);

    MembershipResult acceptInvitation(AuthPrincipal principal, String invitationId);

    List<MembershipAdminView> listMemberships(AuthPrincipal principal, String tenantId);

    MembershipResult updateMembershipStatus(
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        UpdateMembershipStatusCommand command
    );

    MembershipRoleAssignmentsResult listMembershipRoleAssignments(AuthPrincipal principal, String tenantId, String membershipId);

    MembershipAssignableRolesResult listAssignableRoleCodes(AuthPrincipal principal, String tenantId, String membershipId);

    MembershipRoleAssignmentsResult assignMembershipRole(
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        AssignMembershipRoleCommand command
    );

    MembershipRoleAssignmentsResult revokeMembershipRole(
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        RevokeMembershipRoleCommand command
    );

    MembershipPermissionTemplateResult applyPermissionTemplate(
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        ApplyPermissionTemplateCommand command
    );

    MembershipPermissionTemplateResult revokePermissionTemplate(
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        RevokePermissionTemplateCommand command
    );

    GroupAdminResult suspendGroup(AuthPrincipal principal, String tenantId, String groupId);

    GroupAdminResult unsuspendGroup(AuthPrincipal principal, String tenantId, String groupId);

    record InviteCommand(String email, String groupId, MembershipRole role) {
    }

    record UpdateMembershipStatusCommand(MembershipStatus status) {
    }

    record AssignMembershipRoleCommand(String roleCode) {
    }

    record RevokeMembershipRoleCommand(String roleCode) {
    }

    record ApplyPermissionTemplateCommand(String templateCode, String reason) {
    }

    record RevokePermissionTemplateCommand(String templateCode, String reason) {
    }
}

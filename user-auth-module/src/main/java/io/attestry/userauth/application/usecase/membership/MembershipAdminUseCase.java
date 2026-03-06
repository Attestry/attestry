package io.attestry.userauth.application.usecase.membership;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.result.MembershipInvitationResult;
import io.attestry.userauth.application.dto.result.MembershipPermissionTemplateResult;
import io.attestry.userauth.application.dto.result.MembershipAssignableRolesResult;
import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;
import io.attestry.userauth.application.dto.result.TenantAvailableTemplateCodesResult;
import io.attestry.userauth.application.dto.view.MembershipAdminView;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import java.util.List;

public interface MembershipAdminUseCase {
    MembershipInvitationResult invite(ActorContext actor, InviteCommand command);

    MembershipResult acceptInvitation(ActorContext actor, String invitationId);

    List<MembershipAdminView> listMemberships(ActorContext actor);

    MembershipResult updateMembershipStatus(
        ActorContext actor,
        String membershipId,
        UpdateMembershipStatusCommand command
    );

    MembershipRoleAssignmentsResult listMembershipRoleAssignments(ActorContext actor, String membershipId);

    MembershipAssignableRolesResult listAssignableRoleCodes(ActorContext actor, String membershipId);

    TenantAvailableTemplateCodesResult listTenantAvailableTemplateCodes(ActorContext actor);

    MembershipRoleAssignmentsResult assignMembershipRole(
        ActorContext actor,
        String membershipId,
        AssignMembershipRoleCommand command
    );

    MembershipRoleAssignmentsResult revokeMembershipRole(
        ActorContext actor,
        String membershipId,
        RevokeMembershipRoleCommand command
    );

    MembershipPermissionTemplateResult applyPermissionTemplate(
        ActorContext actor,
        String membershipId,
        ApplyPermissionTemplateCommand command
    );

    MembershipPermissionTemplateResult revokePermissionTemplate(
        ActorContext actor,
        String membershipId,
        RevokePermissionTemplateCommand command
    );

    record InviteCommand(String email, MembershipRole role) {
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

package io.attestry.userauth.application.usecase.membership;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.ApplyPermissionTemplateCommand;
import io.attestry.userauth.application.dto.command.AssignMembershipRoleCommand;
import io.attestry.userauth.application.dto.command.InviteCommand;
import io.attestry.userauth.application.dto.command.RevokeMembershipRoleCommand;
import io.attestry.userauth.application.dto.command.RevokePermissionTemplateCommand;
import io.attestry.userauth.application.dto.command.UpdateMembershipStatusCommand;
import io.attestry.userauth.application.dto.result.InvitationResult;
import io.attestry.userauth.application.dto.result.MembershipPermissionTemplateResult;
import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;

public interface MembershipCommandUseCase {
    InvitationResult invite(ActorContext actor, InviteCommand command);

    MembershipResult acceptInvitation(ActorContext actor, String invitationId);

    MembershipResult updateMembershipStatus(
            ActorContext actor,
            String membershipId,
            UpdateMembershipStatusCommand command);

    MembershipRoleAssignmentsResult assignMembershipRole(
            ActorContext actor,
            String membershipId,
            AssignMembershipRoleCommand command);

    MembershipRoleAssignmentsResult revokeMembershipRole(
            ActorContext actor,
            String membershipId,
            RevokeMembershipRoleCommand command);

    MembershipPermissionTemplateResult applyPermissionTemplate(
            ActorContext actor,
            String membershipId,
            ApplyPermissionTemplateCommand command);

    MembershipPermissionTemplateResult revokePermissionTemplate(
            ActorContext actor,
            String membershipId,
            RevokePermissionTemplateCommand command);
}

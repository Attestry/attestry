package io.attestry.userauth.application.membership.command;

import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.membership.command.ApplyPermissionTemplateCommand;
import io.attestry.userauth.application.membership.command.AssignMembershipRoleCommand;
import io.attestry.userauth.application.membership.command.InviteCommand;
import io.attestry.userauth.application.membership.command.RevokeMembershipRoleCommand;
import io.attestry.userauth.application.membership.command.RevokePermissionTemplateCommand;
import io.attestry.userauth.application.membership.command.UpdateMembershipStatusCommand;
import io.attestry.userauth.application.membership.result.InvitationResult;
import io.attestry.userauth.application.membership.result.MembershipPermissionTemplateResult;
import io.attestry.userauth.application.membership.result.MembershipResult;
import io.attestry.userauth.application.membership.result.MembershipRoleAssignmentsResult;

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

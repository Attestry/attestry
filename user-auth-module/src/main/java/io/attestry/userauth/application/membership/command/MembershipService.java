package io.attestry.userauth.application.membership.command;

import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.membership.internal.MembershipInvitationExecutor;
import io.attestry.userauth.application.membership.internal.MembershipRoleAssignmentExecutor;
import io.attestry.userauth.application.membership.internal.MembershipStatusExecutor;
import io.attestry.userauth.application.membership.internal.MembershipTemplateExecutor;
import io.attestry.userauth.application.membership.result.InvitationResult;
import io.attestry.userauth.application.membership.result.MembershipPermissionTemplateResult;
import io.attestry.userauth.application.membership.result.MembershipResult;
import io.attestry.userauth.application.membership.result.MembershipRoleAssignmentsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MembershipService implements MembershipCommandUseCase {

    private final MembershipInvitationExecutor invitationExecutor;
    private final MembershipRoleAssignmentExecutor roleAssignmentExecutor;
    private final MembershipTemplateExecutor templateExecutor;
    private final MembershipStatusExecutor statusExecutor;

    @Override
    @Transactional
    public InvitationResult invite(ActorContext actor, InviteCommand command) {
        return invitationExecutor.invite(actor, command);
    }

    @Override
    @Transactional
    public MembershipResult acceptInvitation(ActorContext actor, String invitationId) {
        return invitationExecutor.acceptInvitation(actor, invitationId);
    }

    @Override
    @Transactional
    public MembershipRoleAssignmentsResult assignMembershipRole(
            ActorContext actor,
            String membershipId,
            AssignMembershipRoleCommand command) {
        return roleAssignmentExecutor.assign(actor, membershipId, command.roleCode());
    }

    @Override
    @Transactional
    public MembershipRoleAssignmentsResult revokeMembershipRole(
            ActorContext actor,
            String membershipId,
            RevokeMembershipRoleCommand command) {
        return roleAssignmentExecutor.revoke(actor, membershipId, command.roleCode());
    }

    @Override
    @Transactional
    public MembershipPermissionTemplateResult applyPermissionTemplate(
            ActorContext actor,
            String membershipId,
            ApplyPermissionTemplateCommand command) {
        return templateExecutor.apply(actor, membershipId, command.templateCode(), command.reason());
    }

    @Override
    @Transactional
    public MembershipPermissionTemplateResult revokePermissionTemplate(
            ActorContext actor,
            String membershipId,
            RevokePermissionTemplateCommand command) {
        return templateExecutor.revoke(actor, membershipId, command.templateCode(), command.reason());
    }

    @Override
    @Transactional
    public MembershipResult updateMembershipStatus(
        ActorContext actor,
        String membershipId,
        UpdateMembershipStatusCommand command
    ) {
        return statusExecutor.updateStatus(actor, membershipId, command);
    }
}

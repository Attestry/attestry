package io.attestry.userauth.application.membership.command;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.UpdateMembershipStatusCommand;
import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.membership.assembler.MembershipResultAssembler;
import io.attestry.userauth.application.membership.policy.MembershipAccessPolicy;
import io.attestry.userauth.application.port.auth.AccessTokenPort;
import io.attestry.userauth.application.port.membership.MembershipPort;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipStatusExecutor {

    private final MembershipPort membershipPort;
    private final MembershipAccessPolicy accessPolicy;
    private final MembershipResultAssembler resultAssembler;
    private final AccessTokenPort accessTokenPort;

    public MembershipResult updateStatus(
        ActorContext actor,
        String membershipId,
        UpdateMembershipStatusCommand command
    ) {
        String tenantId = actor.tenantId();
        Membership current = accessPolicy.loadTenantMembership(membershipId, tenantId);
        if (command.status() == null || command.status() == current.status()) {
            return resultAssembler.toMembershipResult(current);
        }

        accessPolicy.assertLivePermission(
            actor,
            tenantId,
            PermissionCodes.TENANT_MEMBERSHIP_ENFORCE,
            "membership:" + membershipId
        );

        Membership updated = membershipPort.updateMembership(
            tenantId,
            current.membershipId(),
            current.role(),
            command.status()
        );

        if (command.status() == MembershipStatus.SUSPENDED) {
            accessTokenPort.revokeByUserId(current.userId());
        }

        return resultAssembler.toMembershipResult(updated);
    }
}

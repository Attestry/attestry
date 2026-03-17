package io.attestry.userauth.application.membership.command;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;
import io.attestry.userauth.application.membership.policy.ActiveOwnerGuard;
import io.attestry.userauth.application.membership.policy.MembershipAccessPolicy;
import io.attestry.userauth.application.port.auth.AccessTokenPort;
import io.attestry.userauth.application.port.membership.MembershipPort;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.service.RoleAssignmentDomainService;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipRoleAssignmentExecutor {

    private final MembershipPort membershipPort;
    private final MembershipAccessPolicy accessPolicy;
    private final ActiveOwnerGuard activeOwnerGuard;
    private final RoleAssignmentDomainService roleAssignmentDomainService;
    private final ApplicationEventPublisher eventPublisher;
    private final AccessTokenPort accessTokenPort;
    private final Clock clock;

    public MembershipRoleAssignmentsResult assign(ActorContext actor, String membershipId, String roleCode) {
        return mutate(actor, membershipId, roleCode, true);
    }

    public MembershipRoleAssignmentsResult revoke(ActorContext actor, String membershipId, String roleCode) {
        return mutate(actor, membershipId, roleCode, false);
    }

    private MembershipRoleAssignmentsResult mutate(ActorContext actor, String membershipId, String roleCode, boolean assign) {
        String tenantId = actor.tenantId();
        Membership target = accessPolicy.loadTenantMembership(membershipId, tenantId);
        Membership actorMembership = accessPolicy.resolveActiveActorMembership(actor.userId(), tenantId);
        accessPolicy.assertLivePermission(actor, tenantId, PermissionCodes.TENANT_ROLE_ASSIGN, "membership:" + membershipId);

        Instant now = Instant.now(clock);
        if (assign) {
            target.assignRole(
                roleCode,
                actor.userId(),
                now,
                roleAssignmentDomainService,
                actorMembership.currentRoleCodes(),
                actorMembership.membershipId()
            );
        } else {
            activeOwnerGuard.assertCanRevokeRole(target, roleCode);
            target.revokeRole(
                roleCode,
                actor.userId(),
                now,
                roleAssignmentDomainService,
                actorMembership.currentRoleCodes(),
                actorMembership.membershipId()
            );
        }

        membershipPort.save(target);
        target.harvestEvents().forEach(eventPublisher::publishEvent);

        accessTokenPort.revokeByUserId(target.userId());

        return new MembershipRoleAssignmentsResult(membershipId, target.currentRoleCodes().stream().sorted().toList());
    }
}

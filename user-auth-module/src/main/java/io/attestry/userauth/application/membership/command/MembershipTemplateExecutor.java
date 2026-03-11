package io.attestry.userauth.application.membership.command;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.result.MembershipPermissionTemplateResult;
import io.attestry.userauth.application.membership.policy.MembershipAccessPolicy;
import io.attestry.userauth.application.port.auth.AccessTokenPort;
import io.attestry.userauth.application.port.membership.MembershipPort;
import io.attestry.userauth.application.port.template.PermissionTemplatePort;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.authorization.service.TemplateApplicationDomainService;
import io.attestry.userauth.domain.membership.event.TemplatePermissionMutatedEvent;
import io.attestry.userauth.domain.membership.model.Membership;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipTemplateExecutor {

    private final MembershipPort membershipPort;
    private final PermissionTemplatePort permissionTemplatePort;
    private final MembershipAccessPolicy accessPolicy;
    private final TemplateApplicationDomainService templateApplicationDomainService;
    private final ApplicationEventPublisher eventPublisher;
    private final AccessTokenPort accessTokenPort;
    private final Clock clock;

    public MembershipPermissionTemplateResult apply(
        ActorContext actor,
        String membershipId,
        String templateCode,
        String reason
    ) {
        return mutate(actor, membershipId, templateCode, reason, true);
    }

    public MembershipPermissionTemplateResult revoke(
        ActorContext actor,
        String membershipId,
        String templateCode,
        String reason
    ) {
        return mutate(actor, membershipId, templateCode, reason, false);
    }

    private MembershipPermissionTemplateResult mutate(
        ActorContext actor,
        String membershipId,
        String templateCode,
        String reason,
        boolean apply
    ) {
        String tenantId = actor.tenantId();
        Membership targetMembership = accessPolicy.loadTenantMembership(membershipId, tenantId);
        Membership actorMembership = accessPolicy.resolveActiveActorMembership(actor.userId(), tenantId);

        PermissionTemplatePort.PermissionTemplateView template = permissionTemplatePort.findTemplateByCode(templateCode)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.TEMPLATE_NOT_FOUND, "Permission template not found"));

        String normalizedTemplateCode = templateApplicationDomainService.assertCanMutateTemplate(
            actorMembership.currentRoleCodes(),
            templateCode,
            template.enabled()
        );

        accessPolicy.assertLivePermission(
            actor,
            tenantId,
            PermissionCodes.TENANT_ROLE_ASSIGN,
            "membership:" + membershipId + ":template:" + normalizedTemplateCode
        );

        String effectiveReason = reason == null ? normalizedTemplateCode : reason;
        Instant now = Instant.now(clock);
        Set<String> permissionCodes = apply
            ? membershipPort.applyPermissionTemplateToMembership(
                membershipId, normalizedTemplateCode, effectiveReason, actor.userId(), now
            )
            : membershipPort.revokePermissionTemplateFromMembership(membershipId, normalizedTemplateCode);

        eventPublisher.publishEvent(new TemplatePermissionMutatedEvent(
            actor.userId(),
            tenantId,
            membershipId,
            normalizedTemplateCode,
            apply ? TemplatePermissionMutatedEvent.Action.APPLY : TemplatePermissionMutatedEvent.Action.REVOKE,
            now,
            effectiveReason
        ));

        accessTokenPort.revokeByUserId(targetMembership.userId());

        return new MembershipPermissionTemplateResult(
            membershipId,
            normalizedTemplateCode,
            apply ? "APPLY" : "REVOKE",
            permissionCodes.stream().sorted().toList()
        );
    }
}

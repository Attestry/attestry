package io.attestry.userauth.application.membership.command;

import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.membership.result.InvitationResult;
import io.attestry.userauth.application.membership.result.MembershipResult;
import io.attestry.userauth.application.membership.assembler.MembershipResultAssembler;
import io.attestry.userauth.application.port.membership.InvitationPort;
import io.attestry.userauth.application.port.membership.MembershipPort;
import io.attestry.userauth.application.port.notification.NotificationOutboxWritePort;
import io.attestry.userauth.application.port.tenant.TenantRepositoryPort;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.domain.membership.model.InvitationNotificationPayload;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import io.attestry.userauth.domain.membership.model.NotificationType;
import io.attestry.userauth.domain.membership.policy.DefaultMembershipRolePolicy;
import io.attestry.userauth.domain.tenant.model.Tenant;
import io.attestry.userauth.domain.tenant.model.TenantStatus;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipInvitationExecutor {

    private final InvitationPort invitationRepository;
    private final MembershipPort membershipPort;
    private final TenantRepositoryPort tenantRepository;
    private final NotificationOutboxWritePort notificationOutboxRepository;
    private final MembershipResultAssembler resultAssembler;
    private final Clock clock;

    public InvitationResult invite(ActorContext actor, InviteCommand command) {
        Invitation invitation = Invitation.issue(
            actor.tenantId(),
            command.email(),
            command.role(),
            actor.userId(),
            Instant.now(clock)
        );
        Invitation saved = invitationRepository.save(invitation);
        notificationOutboxRepository.save(
            NotificationOutbox.create(
                NotificationType.INVITATION,
                saved.inviteeEmail().value(),
                new InvitationNotificationPayload(saved.invitationId(), saved.tenantId(), saved.inviteeEmail().value()),
                Instant.now(clock)
            )
        );
        return resultAssembler.toInvitationResult(saved);
    }

    public MembershipResult acceptInvitation(ActorContext actor, String invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.INVITATION_NOT_FOUND, "Invitation not found"));

        membershipPort.findByUserIdAndTenantId(actor.userId(), invitation.tenantId())
            .ifPresent(existing -> {
                throw new UserAuthDomainException(
                    UserAuthErrorCode.DUPLICATE_MEMBERSHIP,
                    "Membership already exists for this tenant"
                );
            });

        Tenant tenant = tenantRepository.findById(invitation.tenantId())
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.TENANT_NOT_FOUND, "Tenant not found"));

        Membership membership = Membership.create(
            actor.userId(),
            tenant.tenantId(),
            tenant.type(),
            invitation.role(),
            TenantStatus.ACTIVE
        );
        membershipPort.save(membership);

        String roleCode = DefaultMembershipRolePolicy.resolveGlobalRoleCode(invitation.role(), tenant.type());
        membershipPort.assignRole(membership.membershipId(), roleCode, actor.userId());

        invitation.accept(actor.userId(), Instant.now(clock));
        invitationRepository.save(invitation);

        Membership reloaded = membershipPort.findMembershipById(membership.membershipId()).orElse(membership);
        return resultAssembler.toMembershipResult(reloaded);
    }
}

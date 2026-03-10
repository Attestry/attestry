package io.attestry.userauth.application.membership;

import io.attestry.userauth.application.dto.result.InvitationResult;
import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.dto.result.MembershipPermissionTemplateResult;
import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.ApplyPermissionTemplateCommand;
import io.attestry.userauth.application.dto.command.AssignMembershipRoleCommand;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.InviteCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.command.RevokeMembershipRoleCommand;
import io.attestry.userauth.application.dto.command.RevokePermissionTemplateCommand;
import io.attestry.userauth.application.dto.command.UpdateMembershipStatusCommand;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.port.*;
import io.attestry.userauth.application.usecase.membership.MembershipCommandUseCase;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.service.RoleAssignmentDomainService;
import io.attestry.userauth.domain.authorization.service.TemplateApplicationDomainService;
import io.attestry.userauth.domain.membership.model.InvitationNotificationPayload;
import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import io.attestry.userauth.domain.membership.model.NotificationType;
import io.attestry.userauth.domain.membership.event.TemplatePermissionMutatedEvent;
import io.attestry.userauth.domain.membership.policy.DefaultMembershipRolePolicy;
import io.attestry.userauth.domain.tenant.model.Tenant;
import io.attestry.userauth.domain.tenant.model.TenantStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MembershipService implements MembershipCommandUseCase {

    private final InvitationPort invitationRepository;
    private final MembershipPort membershipPort;
    private final TenantRepositoryPort tenantRepository;
    private final UserAccountRepositoryPort userAccountRepository;
    private final PermissionTemplatePort permissionTemplatePort;
    private final NotificationOutboxRepositoryPort notificationOutboxRepository;
    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;
    private final RoleAssignmentDomainService roleAssignmentDomainService;
    private final TemplateApplicationDomainService templateApplicationDomainService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public InvitationResult invite(ActorContext actor, InviteCommand command) {

        Invitation invitation = Invitation.issue(
                actor.tenantId(),
                command.email(),
                command.role(),
                actor.userId(),
                Instant.now(clock));

        Invitation saved = invitationRepository.save(invitation);

        notificationOutboxRepository.save(
            NotificationOutbox.create(
                NotificationType.INVITATION,
                saved.inviteeEmail().value(),
                new InvitationNotificationPayload(
                    saved.invitationId(), saved.tenantId(), saved.inviteeEmail().value()),
                Instant.now(clock)));

        return toInvitationResult(saved);
    }

    @Override
    @Transactional
    public MembershipResult acceptInvitation(ActorContext actor, String invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.INVITATION_NOT_FOUND, "Invitation not found"));

        membershipPort.findByUserIdAndTenantId(actor.userId(), invitation.tenantId())
                .ifPresent(existing -> {
                    throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_MEMBERSHIP,
                            "Membership already exists for this tenant");
                });

        Tenant tenant = tenantRepository.findById(invitation.tenantId())
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.TENANT_NOT_FOUND, "Tenant not found"));

        Membership membership = Membership.create(
                actor.userId(), tenant.tenantId(), tenant.type(),
                invitation.role(), TenantStatus.ACTIVE);
        membershipPort.save(membership);

        String roleCode = DefaultMembershipRolePolicy.resolveGlobalRoleCode(invitation.role(), tenant.type());
        membershipPort.assignRole(membership.membershipId(), roleCode, actor.userId());

        invitation.accept(actor.userId(), Instant.now(clock));
        invitationRepository.save(invitation);

        Membership reloaded = membershipPort.findMembershipById(membership.membershipId()).orElse(membership);
        return toMembershipResult(reloaded);
    }



    @Override
    @Transactional
    public MembershipRoleAssignmentsResult assignMembershipRole(
            ActorContext actor,
            String membershipId,
            AssignMembershipRoleCommand command) {
        return mutateMembershipRoleAssignment(actor, membershipId, command.roleCode(), true);
    }

    @Override
    @Transactional
    public MembershipRoleAssignmentsResult revokeMembershipRole(
            ActorContext actor,
            String membershipId,
            RevokeMembershipRoleCommand command) {
        return mutateMembershipRoleAssignment(actor, membershipId, command.roleCode(), false);
    }

    @Override
    @Transactional
    public MembershipPermissionTemplateResult applyPermissionTemplate(
            ActorContext actor,
            String membershipId,
            ApplyPermissionTemplateCommand command) {
        return mutatePermissionTemplate(actor, membershipId, command.templateCode(), command.reason(), true);
    }

    @Override
    @Transactional
    public MembershipPermissionTemplateResult revokePermissionTemplate(
            ActorContext actor,
            String membershipId,
            RevokePermissionTemplateCommand command) {
        return mutatePermissionTemplate(actor, membershipId, command.templateCode(), command.reason(), false);
    }

    @Override
    @Transactional
    public MembershipResult updateMembershipStatus(
        ActorContext actor,
        String membershipId,
        UpdateMembershipStatusCommand command
    ) {
        String tenantId = actor.tenantId();
        Membership current = loadTenantMembership(membershipId, tenantId);

        if (command.status() == null || command.status() == current.status()) {
            return toMembershipResult(current);
        }

        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            actor,
            new AuthzEvaluateCommand(
                tenantId,
                PermissionCodes.TENANT_MEMBERSHIP_ENFORCE,
                "membership:" + membershipId,
                PolicyDecisionMode.LIVE_RECHECK
            )
        );
        if (!decision.allowed()) {
            throw new UserAuthDomainException(
                decision.reason() != null ? UserAuthErrorCode.valueOf(decision.reason()) : UserAuthErrorCode.FORBIDDEN_SCOPE,
                "Membership enforce denied by live policy check"
            );
        }

        Membership updated = membershipPort.updateMembership(
            tenantId,
            current.membershipId(),
            current.role(),
            command.status()
        );
        return toMembershipResult(updated);
    }

    private void assertLivePermission(ActorContext actor, String tenantId, String action, String resourceRef) {
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
                actor,
                new AuthzEvaluateCommand(
                        tenantId,
                        action,
                        resourceRef,
                        PolicyDecisionMode.LIVE_RECHECK));
        if (!decision.allowed()) {
            throw new UserAuthDomainException(
                    decision.reason() != null ? UserAuthErrorCode.valueOf(decision.reason()) : UserAuthErrorCode.FORBIDDEN_SCOPE,
                    "Action denied by live policy check");
        }
    }

    private MembershipRoleAssignmentsResult mutateMembershipRoleAssignment(
            ActorContext actor,
            String membershipId,
            String roleCode,
            boolean assign) {
        String tenantId = actor.tenantId();
        Membership target = loadTenantMembership(membershipId, tenantId);
        Membership actorMembership = resolveActiveActorMembership(actor.userId(), tenantId);
        assertLivePermission(actor, tenantId, PermissionCodes.TENANT_ROLE_ASSIGN, "membership:" + membershipId);

        Instant now = Instant.now(clock);
        if (assign) {
            target.assignRole(roleCode, actor.userId(), now,
                    roleAssignmentDomainService, actorMembership.currentRoleCodes(), actorMembership.membershipId());
        } else {
            target.revokeRole(roleCode, actor.userId(), now,
                    roleAssignmentDomainService, actorMembership.currentRoleCodes(), actorMembership.membershipId());
        }

        membershipPort.save(target);
        target.harvestEvents().forEach(eventPublisher::publishEvent);

        return new MembershipRoleAssignmentsResult(
                membershipId,
                target.currentRoleCodes().stream().sorted().toList());
    }

    private MembershipPermissionTemplateResult mutatePermissionTemplate(
            ActorContext actor,
            String membershipId,
            String templateCode,
            String reason,
            boolean apply) {
        String tenantId = actor.tenantId();
        loadTenantMembership(membershipId, tenantId);
        Membership actorMembership = resolveActiveActorMembership(actor.userId(), tenantId);

        String normalizedTemplateCode = normalizeTemplateCode(templateCode);
        PermissionTemplatePort.PermissionTemplateView template = permissionTemplatePort
                .findTemplateByCode(normalizedTemplateCode)
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.TEMPLATE_NOT_FOUND, "Permission template not found"));

        templateApplicationDomainService.assertCanMutateTemplate(
                actorMembership.currentRoleCodes(), normalizedTemplateCode, template.enabled());

        assertLivePermission(actor, tenantId, PermissionCodes.TENANT_ROLE_ASSIGN,
                "membership:" + membershipId + ":template:" + normalizedTemplateCode);

        String effectiveReason = reason == null ? normalizedTemplateCode : reason;
        Instant now = Instant.now(clock);
        Set<String> permissionCodes = apply
                ? membershipPort.applyPermissionTemplateToMembership(
                        membershipId, normalizedTemplateCode, effectiveReason, actor.userId(), now)
                : membershipPort.revokePermissionTemplateFromMembership(
                        membershipId, normalizedTemplateCode);

        publishTemplatePermissionEvent(
                actor.userId(), tenantId, membershipId, normalizedTemplateCode, apply, now, effectiveReason);

        return new MembershipPermissionTemplateResult(
                membershipId, normalizedTemplateCode,
                apply ? "APPLY" : "REVOKE",
                permissionCodes.stream().sorted().toList());
    }

    private String normalizeTemplateCode(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_REQUEST, "templateCode is required");
        }
        return templateCode.trim().toUpperCase(Locale.ROOT);
    }

    private Membership loadTenantMembership(String membershipId, String tenantId) {
        Membership membership = membershipPort.findMembershipById(membershipId)
                .orElseThrow(() -> new UserAuthDomainException(
                        UserAuthErrorCode.MEMBERSHIP_NOT_FOUND,
                        "Membership not found"));
        if (!tenantId.equals(membership.tenantId())) {
            throw new UserAuthDomainException(
                    UserAuthErrorCode.TENANT_ISOLATION_VIOLATION,
                    "Cross-tenant membership access denied");
        }
        return membership;
    }

    private Membership resolveActiveActorMembership(String actorUserId, String tenantId) {
        return membershipPort.findByUserId(actorUserId).stream()
                .filter(Membership::isActive)
                .filter(membership -> tenantId.equals(membership.tenantId()))
                .findFirst()
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.FORBIDDEN_SCOPE,
                        "Actor has no active membership in tenant"));
    }


    private void publishTemplatePermissionEvent(
            String actorUserId,
            String tenantId,
            String membershipId,
            String templateCode,
            boolean applied,
            Instant occurredAt,
            String reason) {
        eventPublisher.publishEvent(new TemplatePermissionMutatedEvent(
                actorUserId,
                tenantId,
                membershipId,
                templateCode,
                applied ? TemplatePermissionMutatedEvent.Action.APPLY : TemplatePermissionMutatedEvent.Action.REVOKE,
                occurredAt,
                reason));
    }

    private InvitationResult toInvitationResult(Invitation invitation) {
        return new InvitationResult(
                invitation.invitationId(),
                invitation.tenantId(),
                invitation.inviteeEmail().value(),
                invitation.role().name(),
                invitation.status().name());
    }

    private MembershipResult toMembershipResult(Membership membership) {
        String email = resolveUserEmail(membership.userId());
        return new MembershipResult(
                membership.membershipId(),
                membership.tenantId(),
                email,
                membership.currentRoleCodes().stream().sorted().toList(),
                membership.status().name());
    }

    private String resolveUserEmail(String userId) {
        return userAccountRepository.findById(userId)
                .map(user -> user.email().value())
                .orElse(null);
    }
}

package io.attestry.userauth.application.membership;

import io.attestry.userauth.application.dto.result.MembershipInvitationResult;
import io.attestry.userauth.application.dto.result.MembershipAssignableRolesResult;
import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.dto.result.MembershipPermissionTemplateResult;
import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;
import io.attestry.userauth.application.dto.result.TenantAvailableTemplateCodesResult;
import io.attestry.userauth.application.dto.view.MembershipAdminView;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.port.InvitationNotificationPort;
import io.attestry.userauth.application.port.MembershipAdminRepositoryPort;
import io.attestry.userauth.application.port.TemplateAdminRepositoryPort;
import io.attestry.userauth.application.port.UserAccountRepositoryPort;
import io.attestry.userauth.domain.membership.repository.MembershipRepository;
import io.attestry.userauth.application.usecase.membership.MembershipAdminUseCase;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.identity.model.UserAccount;
import io.attestry.userauth.domain.membership.service.RoleAssignmentDomainService;
import io.attestry.userauth.domain.authorization.service.TemplateApplicationDomainService;
import io.attestry.userauth.domain.membership.event.TemplatePermissionMutatedEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipAdminService implements MembershipAdminUseCase {
    private static final Logger log = LoggerFactory.getLogger(MembershipAdminService.class);

    private final MembershipAdminRepositoryPort membershipAdminRepository;
    private final MembershipRepository membershipRepository;
    private final UserAccountRepositoryPort userAccountRepository;
    private final TemplateAdminRepositoryPort templateAdminRepository;
    private final InvitationNotificationPort invitationNotificationPort;
    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;
    private final RoleAssignmentDomainService roleAssignmentDomainService;
    private final TemplateApplicationDomainService templateApplicationDomainService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public MembershipAdminService(
            MembershipAdminRepositoryPort membershipAdminRepository,
            MembershipRepository membershipRepository,
            UserAccountRepositoryPort userAccountRepository,
            TemplateAdminRepositoryPort templateAdminRepository,
            InvitationNotificationPort invitationNotificationPort,
            EvaluateAuthorizationUseCase evaluateAuthorizationUseCase,
            RoleAssignmentDomainService roleAssignmentDomainService,
            TemplateApplicationDomainService templateApplicationDomainService,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.membershipAdminRepository = membershipAdminRepository;
        this.membershipRepository = membershipRepository;
        this.userAccountRepository = userAccountRepository;
        this.templateAdminRepository = templateAdminRepository;
        this.invitationNotificationPort = invitationNotificationPort;
        this.evaluateAuthorizationUseCase = evaluateAuthorizationUseCase;
        this.roleAssignmentDomainService = roleAssignmentDomainService;
        this.templateApplicationDomainService = templateApplicationDomainService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    // TODO("이메일 발송로직")
    @Override
    @Transactional
    public MembershipInvitationResult invite(ActorContext actor, InviteCommand command) {
        String tenantId = actor.tenantId();

        Invitation invitation = Invitation.issue(
                tenantId,
                command.email(),
                command.role(),
                actor.userId(),
                Instant.now(clock));
        Invitation saved = membershipAdminRepository.saveInvitation(invitation);
        notifyInvitation(saved);
        return toInvitationResult(saved);
    }

    @Override
    @Transactional
    public MembershipResult acceptInvitation(ActorContext actor, String invitationId) {
        Invitation invitation = membershipAdminRepository.findInvitationById(invitationId)
                .orElseThrow(() -> new DomainException(ErrorCode.INVITATION_NOT_FOUND, "Invitation not found"));

        UserAccount userAccount = userAccountRepository.findByUserId(actor.userId())
                .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "User not found"));

        membershipRepository.findByUserIdAndTenantId(actor.userId(), invitation.tenantId())
                .ifPresent(existing -> {
                    throw new DomainException(ErrorCode.DUPLICATE_MEMBERSHIP,
                            "Membership already exists for this tenant");
                });

        Membership membership = membershipAdminRepository.createMembership(
                actor.userId(),
                invitation.tenantId(),
                invitation.role());
        invitation.accept(actor.userId(), userAccount.email(), Instant.now(clock));
        membershipAdminRepository.saveInvitation(invitation);

        return toMembershipResult(membership);
    }



    @Override
    @Transactional(readOnly = true)
    public List<MembershipAdminView> listMemberships(ActorContext actor) {
        String tenantId = actor.tenantId();
        return membershipAdminRepository.findMembershipsByTenantId(tenantId).stream()
            .map(this::toMembershipView)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipRoleAssignmentsResult listMembershipRoleAssignments(ActorContext actor, String membershipId) {
        String tenantId = actor.tenantId();
        Membership target = membershipAdminRepository.findMembershipById(membershipId)
                .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        if (!tenantId.equals(target.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }
        return new MembershipRoleAssignmentsResult(membershipId, target.currentRoleCodes().stream().sorted().toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipAssignableRolesResult listAssignableRoleCodes(ActorContext actor, String membershipId) {
        String tenantId = actor.tenantId();
        Membership target = membershipAdminRepository.findMembershipById(membershipId)
                .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        if (!tenantId.equals(target.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }

        Membership actorMembership = resolveActiveActorMembership(actor.userId(), tenantId);
        Set<String> actorRoles = actorMembership.currentRoleCodes();
        List<String> assignableRoleCodes = membershipAdminRepository.findGlobalEnabledRoleCodes().stream()
                .map(roleCode -> roleAssignmentDomainService.evaluate(
                        actorRoles,
                        actorMembership.membershipId(),
                        membershipId,
                        roleCode))
                .filter(evaluation -> !evaluation.denied())
                .filter(evaluation -> isRoleAssignableByAuthorization(actor, tenantId, membershipId, evaluation))
                .map(RoleAssignmentDomainService.Evaluation::normalizedRoleCode)
                .sorted()
                .toList();

        return new MembershipAssignableRolesResult(membershipId, assignableRoleCodes);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantAvailableTemplateCodesResult listTenantAvailableTemplateCodes(ActorContext actor) {
        String tenantId = actor.tenantId();
        resolveActiveActorMembership(actor.userId(), tenantId);
        List<String> templateCodes = templateAdminRepository.findTenantRoleTemplateBindings(tenantId).stream()
                .filter(TemplateAdminRepositoryPort.TenantRoleTemplateBindingView::enabled)
                .map(TemplateAdminRepositoryPort.TenantRoleTemplateBindingView::templateCode)
                .distinct()
                .sorted()
                .toList();
        return new TenantAvailableTemplateCodesResult(tenantId, templateCodes);
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
        Membership current = membershipAdminRepository.findMembershipById(membershipId)
            .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));

        if (!tenantId.equals(current.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }

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
            throw new DomainException(
                decision.reason() != null ? ErrorCode.valueOf(decision.reason()) : ErrorCode.FORBIDDEN_SCOPE,
                "Membership enforce denied by live policy check"
            );
        }

        Membership updated = membershipAdminRepository.updateMembership(
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
            throw new DomainException(
                    decision.reason() != null ? ErrorCode.valueOf(decision.reason()) : ErrorCode.FORBIDDEN_SCOPE,
                    "Action denied by live policy check");
        }
    }

    private MembershipRoleAssignmentsResult mutateMembershipRoleAssignment(
            ActorContext actor,
            String membershipId,
            String roleCode,
            boolean assign) {
        String tenantId = actor.tenantId();
        Membership target = membershipAdminRepository.findMembershipById(membershipId)
                .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        if (!tenantId.equals(target.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }

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

        membershipRepository.save(target);
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
        Membership target = membershipAdminRepository.findMembershipById(membershipId)
                .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        if (!tenantId.equals(target.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }

        Membership actorMembership = resolveActiveActorMembership(actor.userId(), tenantId);
        Set<String> actorRoles = actorMembership.currentRoleCodes();
        String normalizedTemplateCode = normalizeTemplateCode(templateCode);
        TemplateAdminRepositoryPort.PermissionTemplateView template = templateAdminRepository
                .findTemplateByCode(normalizedTemplateCode)
                .orElseThrow(() -> new DomainException(ErrorCode.TEMPLATE_NOT_FOUND, "Permission template not found"));
        TemplateApplicationDomainService.Evaluation templateEvaluation = templateApplicationDomainService.evaluate(
                actorRoles,
                normalizedTemplateCode,
                template.enabled());
        if (templateEvaluation.denialReason() == TemplateApplicationDomainService.DenialReason.ACTOR_NOT_ALLOWED) {
            throw new DomainException(ErrorCode.FORBIDDEN_SCOPE, "Permission template operation requires TENANT_OWNER");
        }
        if (templateEvaluation.denialReason() == TemplateApplicationDomainService.DenialReason.TEMPLATE_DISABLED) {
            throw new DomainException(ErrorCode.FORBIDDEN_SCOPE, "Permission template is disabled");
        }
        assertLivePermission(
                actor,
                tenantId,
                PermissionCodes.TENANT_ROLE_ASSIGN,
                "membership:" + membershipId + ":template:" + normalizedTemplateCode);
        Instant now = Instant.now(clock);
        Set<String> permissionCodes;
        if (apply) {
            permissionCodes = membershipAdminRepository.applyPermissionTemplateToMembership(
                    membershipId,
                    normalizedTemplateCode,
                    reason == null ? normalizedTemplateCode : reason,
                    actor.userId(),
                    now);
        } else {
            permissionCodes = membershipAdminRepository.revokePermissionTemplateFromMembership(
                    membershipId,
                    normalizedTemplateCode);
        }
        publishTemplatePermissionEvent(
                actor.userId(),
                tenantId,
                membershipId,
                templateEvaluation.normalizedTemplateCode(),
                apply,
                now,
                reason == null ? normalizedTemplateCode : reason);
        return new MembershipPermissionTemplateResult(
                membershipId,
                templateEvaluation.normalizedTemplateCode(),
                apply ? "APPLY" : "REVOKE",
                permissionCodes.stream().sorted().toList());
    }

    private String normalizeTemplateCode(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "templateCode is required");
        }
        return templateCode.trim().toUpperCase(Locale.ROOT);
    }

    private Membership resolveActiveActorMembership(String actorUserId, String tenantId) {
        return membershipRepository.findByUserId(actorUserId).stream()
                .filter(Membership::isActive)
                .filter(membership -> tenantId.equals(membership.tenantId()))
                .findFirst()
                .orElseThrow(() -> new DomainException(ErrorCode.FORBIDDEN_SCOPE,
                        "Actor has no active membership in tenant"));
    }

    private boolean isRoleAssignableByAuthorization(
            ActorContext actor,
            String tenantId,
            String membershipId,
            RoleAssignmentDomainService.Evaluation roleEvaluation) {
        PolicyDecisionMode mode = roleEvaluation.requiresLiveRecheck()
                ? PolicyDecisionMode.LIVE_RECHECK
                : PolicyDecisionMode.TOKEN_SNAPSHOT;
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
                actor,
                new AuthzEvaluateCommand(
                        tenantId,
                        PermissionCodes.TENANT_ROLE_ASSIGN,
                        "membership:" + membershipId + ":role:" + roleEvaluation.normalizedRoleCode(),
                        mode));
        return decision.allowed();
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

    private MembershipInvitationResult toInvitationResult(Invitation invitation) {
        return new MembershipInvitationResult(
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

    private MembershipAdminView toMembershipView(Membership membership) {
        String email = resolveUserEmail(membership.userId());
        return new MembershipAdminView(
                membership.membershipId(),
                membership.tenantId(),
                email,
                membership.currentRoleCodes().stream().sorted().toList(),
                membership.status().name());
    }

    private String resolveUserEmail(String userId) {
        return userAccountRepository.findByUserId(userId)
                .map(user -> user.email().value())
                .orElse(null);
    }

    private void notifyInvitation(Invitation invitation) {
        try {
            invitationNotificationPort.send(
                    new InvitationNotificationPort.InvitationNotification(
                            invitation.invitationId(),
                            invitation.tenantId(),
                            invitation.inviteeEmail().value()));
        } catch (Exception ex) {
            // Invitation itself is already persisted; keep business flow and log for
            // retry/monitoring.
            log.warn("Invitation email send failed. invitationId={}, tenantId={}, inviteeEmail={}",
                    invitation.invitationId(), invitation.tenantId(), invitation.inviteeEmail().value(), ex);
        }
    }
}

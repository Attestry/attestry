package io.attestry.userauth.application.membership;

import io.attestry.userauth.application.dto.result.MembershipInvitationResult;
import io.attestry.userauth.application.dto.result.MembershipAssignableRolesResult;
import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.dto.result.GroupAdminResult;
import io.attestry.userauth.application.dto.result.MembershipPermissionTemplateResult;
import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;
import io.attestry.userauth.application.dto.result.TenantAvailableTemplateCodesResult;
import io.attestry.userauth.application.dto.view.MembershipAdminView;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.port.MembershipAdminRepositoryPort;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.application.port.TemplateAdminRepositoryPort;
import io.attestry.userauth.application.port.UserAccountRepositoryPort;
import io.attestry.userauth.application.usecase.membership.MembershipAdminUseCase;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.auth.model.PermissionCodes;
import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.organization.model.Group;
import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.user.model.UserAccount;
import io.attestry.userauth.domain.policy.RoleAssignmentDomainService;
import io.attestry.userauth.domain.policy.TenantIsolationPolicy;
import io.attestry.userauth.domain.policy.TemplateApplicationDomainService;
import io.attestry.userauth.domain.membership.event.RoleAssignmentAuditedEvent;
import io.attestry.userauth.domain.membership.event.TemplatePermissionMutatedEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipAdminService implements MembershipAdminUseCase {

    private final MembershipAdminRepositoryPort membershipAdminRepository;
    private final MembershipRepositoryPort membershipRepository;
    private final UserAccountRepositoryPort userAccountRepository;
    private final TemplateAdminRepositoryPort templateAdminRepository;
    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;
    private final RoleAssignmentDomainService roleAssignmentDomainService;
    private final TemplateApplicationDomainService templateApplicationDomainService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public MembershipAdminService(
        MembershipAdminRepositoryPort membershipAdminRepository,
        MembershipRepositoryPort membershipRepository,
        UserAccountRepositoryPort userAccountRepository,
        TemplateAdminRepositoryPort templateAdminRepository,
        EvaluateAuthorizationUseCase evaluateAuthorizationUseCase,
        RoleAssignmentDomainService roleAssignmentDomainService,
        TemplateApplicationDomainService templateApplicationDomainService,
        ApplicationEventPublisher eventPublisher,
        Clock clock
    ) {
        this.membershipAdminRepository = membershipAdminRepository;
        this.membershipRepository = membershipRepository;
        this.userAccountRepository = userAccountRepository;
        this.templateAdminRepository = templateAdminRepository;
        this.evaluateAuthorizationUseCase = evaluateAuthorizationUseCase;
        this.roleAssignmentDomainService = roleAssignmentDomainService;
        this.templateApplicationDomainService = templateApplicationDomainService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    // TODO("이메일 발송로직")
    @Override
    @Transactional
    public MembershipInvitationResult invite(ActorContext actor, String tenantId, InviteCommand command) {
        assertTenantIsolation(actor, tenantId);
        MembershipAdminRepositoryPort.GroupView group = membershipAdminRepository.findGroupById(command.groupId())
            .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Group not found"));

        if (!tenantId.equals(group.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant group access denied");
        }

        Invitation invitation = Invitation.issue(
            tenantId,
            command.groupId(),
            command.email(),
            command.role(),
            actor.userId(),
            Instant.now(clock)
        );
        return toInvitationResult(membershipAdminRepository.saveInvitation(invitation));
    }

    @Override
    @Transactional
    public MembershipResult acceptInvitation(ActorContext actor, String invitationId) {
        Invitation invitation = membershipAdminRepository.findInvitationById(invitationId)
            .orElseThrow(() -> new DomainException(ErrorCode.INVITATION_NOT_FOUND, "Invitation not found"));

        UserAccount userAccount = userAccountRepository.findByUserId(actor.userId())
            .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "User not found"));

        Membership membership = membershipAdminRepository.createMembership(
            actor.userId(),
            invitation.groupId(),
            invitation.tenantId(),
            invitation.role()
        );
        membershipAdminRepository.saveInvitation(invitation.accept(actor.userId(), userAccount.user().email(), Instant.now(clock)));

        return toMembershipResult(membership);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipAdminView> listMemberships(ActorContext actor, String tenantId) {
        assertTenantIsolation(actor, tenantId);
        return membershipAdminRepository.findMembershipsByTenantId(tenantId).stream()
            .map(this::toMembershipView)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipRoleAssignmentsResult listMembershipRoleAssignments(ActorContext actor, String tenantId, String membershipId) {
        assertTenantIsolation(actor, tenantId);
        Membership target = membershipAdminRepository.findMembershipById(membershipId)
            .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        if (!tenantId.equals(target.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }
        Set<String> roleCodes = membershipAdminRepository.findRoleCodesByMembershipId(membershipId);
        return new MembershipRoleAssignmentsResult(membershipId, roleCodes.stream().sorted().toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipAssignableRolesResult listAssignableRoleCodes(ActorContext actor, String tenantId, String membershipId) {
        assertTenantIsolation(actor, tenantId);
        Membership target = membershipAdminRepository.findMembershipById(membershipId)
            .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        if (!tenantId.equals(target.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }

        Membership actorMembership = resolveActiveActorMembership(actor.userId(), tenantId);
        Set<String> actorRoles = membershipAdminRepository.findRoleCodesByMembershipId(actorMembership.membershipId());
        List<String> assignableRoleCodes = membershipAdminRepository.findGlobalEnabledRoleCodes().stream()
            .map(roleCode -> roleAssignmentDomainService.evaluate(
                actorRoles,
                actorMembership.membershipId(),
                membershipId,
                roleCode
            ))
            .filter(evaluation -> !evaluation.denied())
            .filter(evaluation -> isRoleAssignableByAuthorization(actor, tenantId, membershipId, evaluation))
            .map(RoleAssignmentDomainService.Evaluation::normalizedRoleCode)
            .sorted()
            .toList();

        return new MembershipAssignableRolesResult(membershipId, assignableRoleCodes);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantAvailableTemplateCodesResult listTenantAvailableTemplateCodes(ActorContext actor, String tenantId) {
        assertTenantIsolation(actor, tenantId);
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
        String tenantId,
        String membershipId,
        AssignMembershipRoleCommand command
    ) {
        return mutateMembershipRoleAssignment(actor, tenantId, membershipId, command.roleCode(), true);
    }

    @Override
    @Transactional
    public MembershipRoleAssignmentsResult revokeMembershipRole(
        ActorContext actor,
        String tenantId,
        String membershipId,
        RevokeMembershipRoleCommand command
    ) {
        return mutateMembershipRoleAssignment(actor, tenantId, membershipId, command.roleCode(), false);
    }

    @Override
    @Transactional
    public MembershipPermissionTemplateResult applyPermissionTemplate(
        ActorContext actor,
        String tenantId,
        String membershipId,
        ApplyPermissionTemplateCommand command
    ) {
        return mutatePermissionTemplate(actor, tenantId, membershipId, command.templateCode(), command.reason(), true);
    }

    @Override
    @Transactional
    public MembershipPermissionTemplateResult revokePermissionTemplate(
        ActorContext actor,
        String tenantId,
        String membershipId,
        RevokePermissionTemplateCommand command
    ) {
        return mutatePermissionTemplate(actor, tenantId, membershipId, command.templateCode(), command.reason(), false);
    }

    @Override
    @Transactional
    public MembershipResult updateMembershipStatus(
        ActorContext actor,
        String tenantId,
        String membershipId,
        UpdateMembershipStatusCommand command
    ) {
        assertTenantIsolation(actor, tenantId);
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

    @Override
    @Transactional
    public GroupAdminResult suspendGroup(ActorContext actor, String tenantId, String groupId) {
        assertTenantIsolation(actor, tenantId);
        assertLivePermission(actor, tenantId, PermissionCodes.TENANT_GROUP_SUSPEND, "group:" + groupId);
        MembershipAdminRepositoryPort.GroupView groupView = membershipAdminRepository.findGroupById(groupId)
            .orElseThrow(() -> new DomainException(ErrorCode.GROUP_NOT_FOUND, "Group not found"));

        if (!tenantId.equals(groupView.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant group access denied");
        }

        Group group = new Group(groupView.groupId(), groupView.tenantId(), groupView.type(), groupView.status()).suspend();
        MembershipAdminRepositoryPort.GroupView saved = membershipAdminRepository.saveGroup(
            new MembershipAdminRepositoryPort.GroupView(group.groupId(), group.tenantId(), group.type(), group.status())
        );
        membershipAdminRepository.updateGroupStatusOnMemberships(saved.groupId(), GroupStatus.SUSPENDED);
        return toGroupResult(saved);
    }

    @Override
    @Transactional
    public GroupAdminResult unsuspendGroup(ActorContext actor, String tenantId, String groupId) {
        assertTenantIsolation(actor, tenantId);
        MembershipAdminRepositoryPort.GroupView groupView = membershipAdminRepository.findGroupById(groupId)
            .orElseThrow(() -> new DomainException(ErrorCode.GROUP_NOT_FOUND, "Group not found"));
        if (!tenantId.equals(groupView.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant group access denied");
        }
        Group group = new Group(groupView.groupId(), groupView.tenantId(), groupView.type(), groupView.status()).unsuspend();
        MembershipAdminRepositoryPort.GroupView saved = membershipAdminRepository.saveGroup(
            new MembershipAdminRepositoryPort.GroupView(group.groupId(), group.tenantId(), group.type(), group.status())
        );
        membershipAdminRepository.updateGroupStatusOnMemberships(saved.groupId(), GroupStatus.ACTIVE);
        return toGroupResult(saved);
    }

    private void assertTenantIsolation(ActorContext actor, String tenantId) {
        if (!TenantIsolationPolicy.isIsolated(actor.tenantId(), tenantId)) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant access denied");
        }
    }

    private void assertLivePermission(ActorContext actor, String tenantId, String action, String resourceRef) {
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            actor,
            new AuthzEvaluateCommand(
                tenantId,
                action,
                resourceRef,
                PolicyDecisionMode.LIVE_RECHECK
            )
        );
        if (!decision.allowed()) {
            throw new DomainException(
                decision.reason() != null ? ErrorCode.valueOf(decision.reason()) : ErrorCode.FORBIDDEN_SCOPE,
                "Action denied by live policy check"
            );
        }
    }

    private MembershipRoleAssignmentsResult mutateMembershipRoleAssignment(
        ActorContext actor,
        String tenantId,
        String membershipId,
        String roleCode,
        boolean assign
    ) {
        assertTenantIsolation(actor, tenantId);
        Membership target = membershipAdminRepository.findMembershipById(membershipId)
            .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        if (!tenantId.equals(target.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }

        Membership actorMembership = resolveActiveActorMembership(actor.userId(), tenantId);
        Set<String> actorRoles = membershipAdminRepository.findRoleCodesByMembershipId(actorMembership.membershipId());
        RoleAssignmentDomainService.Evaluation roleEvaluation = roleAssignmentDomainService.evaluate(
            actorRoles,
            actorMembership.membershipId(),
            membershipId,
            roleCode
        );
        String normalizedRoleCode = roleEvaluation.normalizedRoleCode();
        if (roleEvaluation.denialReason() == RoleAssignmentDomainService.DenialReason.INVALID_ROLE_CODE) {
            throw new DomainException(ErrorCode.ROLE_NOT_FOUND, "Role code is required");
        }

        Instant requestedAt = Instant.now(clock);
        if (roleEvaluation.denialReason() == RoleAssignmentDomainService.DenialReason.SELF_ESCALATION_DENIED) {
            publishRoleAuditEvent(actor.userId(), tenantId, membershipId, normalizedRoleCode, assign, "POLICY_CHECK", false, ErrorCode.FORBIDDEN_SCOPE.name(), requestedAt);
            throw new DomainException(ErrorCode.FORBIDDEN_SCOPE, "Self escalation is not allowed");
        }
        if (roleEvaluation.denialReason() == RoleAssignmentDomainService.DenialReason.NOT_ASSIGNABLE) {
            publishRoleAuditEvent(actor.userId(), tenantId, membershipId, normalizedRoleCode, assign, "POLICY_CHECK", false, ErrorCode.FORBIDDEN_SCOPE.name(), requestedAt);
            throw new DomainException(ErrorCode.FORBIDDEN_SCOPE, "Role assignment is not allowed");
        }

        PolicyDecisionMode mode = roleEvaluation.decisionMode();
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            actor,
            new AuthzEvaluateCommand(
                tenantId,
                PermissionCodes.TENANT_ROLE_ASSIGN,
                "membership:" + membershipId + ":role:" + normalizedRoleCode,
                mode
            )
        );
        if (!decision.allowed()) {
            publishRoleAuditEvent(actor.userId(), tenantId, membershipId, normalizedRoleCode, assign, mode.name(), false, decision.reason(), requestedAt);
            throw new DomainException(
                decision.reason() != null ? ErrorCode.valueOf(decision.reason()) : ErrorCode.FORBIDDEN_SCOPE,
                "Role assignment denied by policy check"
            );
        }

        Set<String> updatedRoleCodes = assign
            ? membershipAdminRepository.assignRoleToMembership(membershipId, normalizedRoleCode, actor.userId(), Instant.now(clock))
            : membershipAdminRepository.revokeRoleFromMembership(membershipId, normalizedRoleCode);
        publishRoleAuditEvent(actor.userId(), tenantId, membershipId, normalizedRoleCode, assign, mode.name(), true, null, requestedAt);
        return new MembershipRoleAssignmentsResult(
            membershipId,
            new LinkedHashSet<>(updatedRoleCodes).stream().sorted().toList()
        );
    }

    private MembershipPermissionTemplateResult mutatePermissionTemplate(
        ActorContext actor,
        String tenantId,
        String membershipId,
        String templateCode,
        String reason,
        boolean apply
    ) {
        assertTenantIsolation(actor, tenantId);
        Membership target = membershipAdminRepository.findMembershipById(membershipId)
            .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        if (!tenantId.equals(target.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }

        Membership actorMembership = resolveActiveActorMembership(actor.userId(), tenantId);
        Set<String> actorRoles = membershipAdminRepository.findRoleCodesByMembershipId(actorMembership.membershipId());
        String normalizedTemplateCode = normalizeTemplateCode(templateCode);
        TemplateAdminRepositoryPort.PermissionTemplateView template = templateAdminRepository.findTemplateByCode(normalizedTemplateCode)
            .orElseThrow(() -> new DomainException(ErrorCode.TEMPLATE_NOT_FOUND, "Permission template not found"));
        TemplateApplicationDomainService.Evaluation templateEvaluation = templateApplicationDomainService.evaluate(
            actorRoles,
            normalizedTemplateCode,
            template.enabled()
        );
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
            "membership:" + membershipId + ":template:" + normalizedTemplateCode
        );
        Instant now = Instant.now(clock);
        Set<String> permissionCodes;
        if (apply) {
            permissionCodes = membershipAdminRepository.applyPermissionTemplateToMembership(
                membershipId,
                normalizedTemplateCode,
                reason == null ? normalizedTemplateCode : reason,
                actor.userId(),
                now
            );
        } else {
            permissionCodes = membershipAdminRepository.revokePermissionTemplateFromMembership(
                membershipId,
                normalizedTemplateCode
            );
        }
        publishTemplatePermissionEvent(
            actor.userId(),
            tenantId,
            membershipId,
            templateEvaluation.normalizedTemplateCode(),
            apply,
            now,
            reason == null ? normalizedTemplateCode : reason
        );
        return new MembershipPermissionTemplateResult(
            membershipId,
            templateEvaluation.normalizedTemplateCode(),
            apply ? "APPLY" : "REVOKE",
            permissionCodes.stream().sorted().toList()
        );
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
            .orElseThrow(() -> new DomainException(ErrorCode.FORBIDDEN_SCOPE, "Actor has no active membership in tenant"));
    }

    private boolean isRoleAssignableByAuthorization(
        ActorContext actor,
        String tenantId,
        String membershipId,
        RoleAssignmentDomainService.Evaluation roleEvaluation
    ) {
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            actor,
            new AuthzEvaluateCommand(
                tenantId,
                PermissionCodes.TENANT_ROLE_ASSIGN,
                "membership:" + membershipId + ":role:" + roleEvaluation.normalizedRoleCode(),
                roleEvaluation.decisionMode()
            )
        );
        return decision.allowed();
    }

    private void publishRoleAuditEvent(
        String actorUserId,
        String tenantId,
        String membershipId,
        String roleCode,
        boolean assign,
        String decisionSource,
        boolean allowed,
        String reasonCode,
        Instant requestedAt
    ) {
        eventPublisher.publishEvent(new RoleAssignmentAuditedEvent(
            actorUserId,
            tenantId,
            membershipId,
            assign ? null : roleCode,
            assign ? roleCode : null,
            decisionSource,
            allowed,
            reasonCode,
            requestedAt,
            Instant.now(clock)
        ));
    }

    private void publishTemplatePermissionEvent(
        String actorUserId,
        String tenantId,
        String membershipId,
        String templateCode,
        boolean applied,
        Instant occurredAt,
        String reason
    ) {
        eventPublisher.publishEvent(new TemplatePermissionMutatedEvent(
            actorUserId,
            tenantId,
            membershipId,
            templateCode,
            applied ? TemplatePermissionMutatedEvent.Action.APPLY : TemplatePermissionMutatedEvent.Action.REVOKE,
            occurredAt,
            reason
        ));
    }

    private MembershipInvitationResult toInvitationResult(Invitation invitation) {
        return new MembershipInvitationResult(
            invitation.invitationId(),
            invitation.tenantId(),
            invitation.groupId(),
            invitation.inviteeEmail().value(),
            invitation.role().name(),
            invitation.status().name()
        );
    }

    private MembershipResult toMembershipResult(Membership membership) {
        List<String> roleCodes = membershipAdminRepository.findRoleCodesByMembershipId(membership.membershipId()).stream().sorted().toList();
        return new MembershipResult(
            membership.membershipId(),
            membership.tenantId(),
            membership.groupId(),
            roleCodes,
            membership.status().name()
        );
    }

    private MembershipAdminView toMembershipView(Membership membership) {
        List<String> roleCodes = membershipAdminRepository.findRoleCodesByMembershipId(membership.membershipId()).stream().sorted().toList();
        return new MembershipAdminView(
            membership.membershipId(),
            membership.tenantId(),
            membership.groupId(),
            roleCodes,
            membership.status().name()
        );
    }

    private GroupAdminResult toGroupResult(MembershipAdminRepositoryPort.GroupView group) {
        return new GroupAdminResult(
            group.groupId(),
            group.tenantId(),
            group.type().name(),
            group.status().name()
        );
    }

}

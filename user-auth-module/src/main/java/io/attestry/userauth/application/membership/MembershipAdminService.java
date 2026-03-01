package io.attestry.userauth.application.membership;

import io.attestry.userauth.application.dto.result.MembershipInvitationResult;
import io.attestry.userauth.application.dto.result.MembershipAssignableRolesResult;
import io.attestry.userauth.application.dto.result.MembershipResult;
import io.attestry.userauth.application.dto.result.GroupAdminResult;
import io.attestry.userauth.application.dto.result.MembershipPermissionTemplateResult;
import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;
import io.attestry.userauth.application.dto.view.MembershipAdminView;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.port.MembershipAdminRepositoryPort;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.application.port.RoleAssignmentAuditPort;
import io.attestry.userauth.application.port.UserAccountRepositoryPort;
import io.attestry.userauth.application.usecase.membership.MembershipAdminUseCase;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.auth.model.PermissionCodes;
import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.organization.model.Group;
import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.user.model.UserAccount;
import io.attestry.userauth.domain.policy.TenantIsolationPolicy;
import io.attestry.userauth.domain.policy.RoleAssignmentPolicy;
import io.attestry.userauth.domain.policy.PermissionTemplatePolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipAdminService implements MembershipAdminUseCase {

    private final MembershipAdminRepositoryPort membershipAdminRepository;
    private final MembershipRepositoryPort membershipRepository;
    private final UserAccountRepositoryPort userAccountRepository;
    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;
    private final RoleAssignmentAuditPort roleAssignmentAuditPort;
    private final Clock clock;

    public MembershipAdminService(
        MembershipAdminRepositoryPort membershipAdminRepository,
        MembershipRepositoryPort membershipRepository,
        UserAccountRepositoryPort userAccountRepository,
        EvaluateAuthorizationUseCase evaluateAuthorizationUseCase,
        RoleAssignmentAuditPort roleAssignmentAuditPort,
        Clock clock
    ) {
        this.membershipAdminRepository = membershipAdminRepository;
        this.membershipRepository = membershipRepository;
        this.userAccountRepository = userAccountRepository;
        this.evaluateAuthorizationUseCase = evaluateAuthorizationUseCase;
        this.roleAssignmentAuditPort = roleAssignmentAuditPort;
        this.clock = clock;
    }

    // TODO("이메일 발송로직")
    @Override
    @Transactional
    public MembershipInvitationResult invite(AuthPrincipal principal, String tenantId, InviteCommand command) {
        assertTenantIsolation(principal, tenantId);
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
            principal.userId(),
            Instant.now(clock)
        );
        return toInvitationResult(membershipAdminRepository.saveInvitation(invitation));
    }

    @Override
    @Transactional
    public MembershipResult acceptInvitation(AuthPrincipal principal, String invitationId) {
        Invitation invitation = membershipAdminRepository.findInvitationById(invitationId)
            .orElseThrow(() -> new DomainException(ErrorCode.INVITATION_NOT_FOUND, "Invitation not found"));

        UserAccount userAccount = userAccountRepository.findByUserId(principal.userId())
            .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "User not found"));

        Membership membership = membershipAdminRepository.createMembership(
            principal.userId(),
            invitation.groupId(),
            invitation.tenantId(),
            invitation.role()
        );
        membershipAdminRepository.saveInvitation(invitation.accept(principal.userId(), userAccount.user().email(), Instant.now(clock)));

        return toMembershipResult(membership);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipAdminView> listMemberships(AuthPrincipal principal, String tenantId) {
        assertTenantIsolation(principal, tenantId);
        return membershipAdminRepository.findMembershipsByTenantId(tenantId).stream()
            .map(this::toMembershipView)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipRoleAssignmentsResult listMembershipRoleAssignments(AuthPrincipal principal, String tenantId, String membershipId) {
        assertTenantIsolation(principal, tenantId);
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
    public MembershipAssignableRolesResult listAssignableRoleCodes(AuthPrincipal principal, String tenantId, String membershipId) {
        assertTenantIsolation(principal, tenantId);
        Membership target = membershipAdminRepository.findMembershipById(membershipId)
            .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        if (!tenantId.equals(target.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }

        Membership actorMembership = resolveActiveActorMembership(principal.userId(), tenantId);
        Set<String> actorRoles = membershipAdminRepository.findRoleCodesByMembershipId(actorMembership.membershipId());
        List<String> assignableRoleCodes = membershipAdminRepository.findGlobalEnabledRoleCodes().stream()
            .filter(roleCode -> RoleAssignmentPolicy.canAssign(actorRoles, roleCode))
            .filter(roleCode -> !RoleAssignmentPolicy.isSelfEscalationDenied(actorMembership.membershipId(), membershipId, roleCode))
            .filter(roleCode -> isRoleAssignableByAuthorization(principal, tenantId, membershipId, roleCode))
            .sorted()
            .toList();

        return new MembershipAssignableRolesResult(membershipId, assignableRoleCodes);
    }

    @Override
    @Transactional
    public MembershipRoleAssignmentsResult assignMembershipRole(
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        AssignMembershipRoleCommand command
    ) {
        return mutateMembershipRoleAssignment(principal, tenantId, membershipId, command.roleCode(), true);
    }

    @Override
    @Transactional
    public MembershipRoleAssignmentsResult revokeMembershipRole(
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        RevokeMembershipRoleCommand command
    ) {
        return mutateMembershipRoleAssignment(principal, tenantId, membershipId, command.roleCode(), false);
    }

    @Override
    @Transactional
    public MembershipPermissionTemplateResult applyPermissionTemplate(
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        ApplyPermissionTemplateCommand command
    ) {
        return mutatePermissionTemplate(principal, tenantId, membershipId, command.templateCode(), command.reason(), true);
    }

    @Override
    @Transactional
    public MembershipPermissionTemplateResult revokePermissionTemplate(
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        RevokePermissionTemplateCommand command
    ) {
        return mutatePermissionTemplate(principal, tenantId, membershipId, command.templateCode(), command.reason(), false);
    }

    @Override
    @Transactional
    public MembershipResult updateMembershipStatus(
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        UpdateMembershipStatusCommand command
    ) {
        assertTenantIsolation(principal, tenantId);
        Membership current = membershipAdminRepository.findMembershipById(membershipId)
            .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));

        if (!tenantId.equals(current.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }

        if (command.status() == null || command.status() == current.status()) {
            return toMembershipResult(current);
        }

        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            principal,
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
    public GroupAdminResult suspendGroup(AuthPrincipal principal, String tenantId, String groupId) {
        assertTenantIsolation(principal, tenantId);
        assertLivePermission(principal, tenantId, PermissionCodes.TENANT_GROUP_SUSPEND, "group:" + groupId);
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
    public GroupAdminResult unsuspendGroup(AuthPrincipal principal, String tenantId, String groupId) {
        assertTenantIsolation(principal, tenantId);
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

    private void assertTenantIsolation(AuthPrincipal principal, String tenantId) {
        if (!TenantIsolationPolicy.isIsolated(principal.tenantId(), tenantId)) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant access denied");
        }
    }

    private void assertLivePermission(AuthPrincipal principal, String tenantId, String action, String resourceRef) {
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            principal,
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
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        String roleCode,
        boolean assign
    ) {
        String normalizedRoleCode = roleCode == null ? null : roleCode.trim().toUpperCase(Locale.ROOT);
        if (normalizedRoleCode == null || normalizedRoleCode.isBlank()) {
            throw new DomainException(ErrorCode.ROLE_NOT_FOUND, "Role code is required");
        }
        assertTenantIsolation(principal, tenantId);
        Membership target = membershipAdminRepository.findMembershipById(membershipId)
            .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        if (!tenantId.equals(target.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }

        Membership actorMembership = resolveActiveActorMembership(principal.userId(), tenantId);
        Set<String> actorRoles = membershipAdminRepository.findRoleCodesByMembershipId(actorMembership.membershipId());

        Instant requestedAt = Instant.now(clock);
        if (RoleAssignmentPolicy.isSelfEscalationDenied(actorMembership.membershipId(), membershipId, normalizedRoleCode)) {
            roleAssignmentAuditPort.log(
                principal.userId(),
                tenantId,
                membershipId,
                assign ? null : normalizedRoleCode,
                assign ? normalizedRoleCode : null,
                "POLICY_CHECK",
                false,
                ErrorCode.FORBIDDEN_SCOPE.name(),
                requestedAt,
                Instant.now(clock)
            );
            throw new DomainException(ErrorCode.FORBIDDEN_SCOPE, "Self escalation is not allowed");
        }
        if (!RoleAssignmentPolicy.canAssign(actorRoles, normalizedRoleCode)) {
            roleAssignmentAuditPort.log(
                principal.userId(),
                tenantId,
                membershipId,
                assign ? null : normalizedRoleCode,
                assign ? normalizedRoleCode : null,
                "POLICY_CHECK",
                false,
                ErrorCode.FORBIDDEN_SCOPE.name(),
                requestedAt,
                Instant.now(clock)
            );
            throw new DomainException(ErrorCode.FORBIDDEN_SCOPE, "Role assignment is not allowed");
        }

        PolicyDecisionMode mode = RoleAssignmentPolicy.requiresLiveRecheck(normalizedRoleCode)
            ? PolicyDecisionMode.LIVE_RECHECK
            : PolicyDecisionMode.TOKEN_SNAPSHOT;
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            principal,
            new AuthzEvaluateCommand(
                tenantId,
                PermissionCodes.TENANT_ROLE_ASSIGN,
                "membership:" + membershipId + ":role:" + normalizedRoleCode,
                mode
            )
        );
        if (!decision.allowed()) {
            roleAssignmentAuditPort.log(
                principal.userId(),
                tenantId,
                membershipId,
                assign ? null : normalizedRoleCode,
                assign ? normalizedRoleCode : null,
                mode.name(),
                false,
                decision.reason(),
                requestedAt,
                Instant.now(clock)
            );
            throw new DomainException(
                decision.reason() != null ? ErrorCode.valueOf(decision.reason()) : ErrorCode.FORBIDDEN_SCOPE,
                "Role assignment denied by policy check"
            );
        }

        Set<String> updatedRoleCodes = assign
            ? membershipAdminRepository.assignRoleToMembership(membershipId, normalizedRoleCode, principal.userId(), Instant.now(clock))
            : membershipAdminRepository.revokeRoleFromMembership(membershipId, normalizedRoleCode);
        roleAssignmentAuditPort.log(
            principal.userId(),
            tenantId,
            membershipId,
            assign ? null : normalizedRoleCode,
            assign ? normalizedRoleCode : null,
            mode.name(),
            true,
            null,
            requestedAt,
            Instant.now(clock)
        );
        return new MembershipRoleAssignmentsResult(
            membershipId,
            new LinkedHashSet<>(updatedRoleCodes).stream().sorted().toList()
        );
    }

    private MembershipPermissionTemplateResult mutatePermissionTemplate(
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        String templateCode,
        String reason,
        boolean apply
    ) {
        assertTenantIsolation(principal, tenantId);
        Membership target = membershipAdminRepository.findMembershipById(membershipId)
            .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        if (!tenantId.equals(target.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }

        Membership actorMembership = resolveActiveActorMembership(principal.userId(), tenantId);
        Set<String> actorRoles = membershipAdminRepository.findRoleCodesByMembershipId(actorMembership.membershipId());
        if (!actorRoles.contains(io.attestry.userauth.domain.auth.model.RoleCodes.TENANT_OWNER)
            && !actorRoles.contains(io.attestry.userauth.domain.auth.model.RoleCodes.PLATFORM_SUPER_ADMIN)) {
            throw new DomainException(ErrorCode.FORBIDDEN_SCOPE, "Permission template operation requires TENANT_OWNER");
        }

        String normalizedTemplateCode = PermissionTemplatePolicy.normalize(templateCode);
        assertLivePermission(
            principal,
            tenantId,
            PermissionCodes.TENANT_ROLE_ASSIGN,
            "membership:" + membershipId + ":template:" + normalizedTemplateCode
        );
        Set<String> permissionCodes = new LinkedHashSet<>(PermissionTemplatePolicy.resolvePermissionCodes(normalizedTemplateCode));
        Instant now = Instant.now(clock);
        if (apply) {
            membershipAdminRepository.upsertPermissionOverrides(
                membershipId,
                permissionCodes,
                "TEMPLATE",
                reason == null ? normalizedTemplateCode : reason,
                principal.userId(),
                now
            );
        } else {
            membershipAdminRepository.deletePermissionOverrides(membershipId, permissionCodes);
        }
        return new MembershipPermissionTemplateResult(
            membershipId,
            normalizedTemplateCode,
            apply ? "APPLY" : "REVOKE",
            permissionCodes.stream().sorted().toList()
        );
    }

    private Membership resolveActiveActorMembership(String actorUserId, String tenantId) {
        return membershipRepository.findByUserId(actorUserId).stream()
            .filter(Membership::isActive)
            .filter(membership -> tenantId.equals(membership.tenantId()))
            .findFirst()
            .orElseThrow(() -> new DomainException(ErrorCode.FORBIDDEN_SCOPE, "Actor has no active membership in tenant"));
    }

    private boolean isRoleAssignableByAuthorization(
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        String roleCode
    ) {
        PolicyDecisionMode mode = RoleAssignmentPolicy.requiresLiveRecheck(roleCode)
            ? PolicyDecisionMode.LIVE_RECHECK
            : PolicyDecisionMode.TOKEN_SNAPSHOT;
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            principal,
            new AuthzEvaluateCommand(
                tenantId,
                PermissionCodes.TENANT_ROLE_ASSIGN,
                "membership:" + membershipId + ":role:" + roleCode,
                mode
            )
        );
        return decision.allowed();
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

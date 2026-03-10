package io.attestry.userauth.application.membership;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.dto.result.MembershipAssignableRolesResult;
import io.attestry.userauth.application.dto.result.MembershipDetailResult;
import io.attestry.userauth.application.dto.result.MembershipRoleAssignmentsResult;
import io.attestry.userauth.application.dto.result.TenantAvailableTemplateCodesResult;
import io.attestry.userauth.application.dto.view.MembershipAdminView;
import io.attestry.userauth.application.dto.view.MembershipView;
import io.attestry.userauth.application.port.MembershipPort;
import io.attestry.userauth.application.port.MembershipProjectionPort;
import io.attestry.userauth.application.port.TenantRoleTemplateBindingPort;
import io.attestry.userauth.application.port.TenantRepositoryPort;
import io.attestry.userauth.application.port.UserAccountRepositoryPort;
import io.attestry.userauth.application.usecase.membership.MembershipQueryUseCase;
import java.util.List;
import java.util.Set;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.identity.model.UserAccount;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.service.RoleAssignmentDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MembershipQueryService implements MembershipQueryUseCase {

    private final MembershipPort membershipPort;
    private final MembershipProjectionPort membershipProjectionPort;
    private final TenantRepositoryPort tenantRepository;
    private final UserAccountRepositoryPort userAccountRepository;
    private final TenantRoleTemplateBindingPort tenantRoleTemplateBindingPort;
    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;
    private final RoleAssignmentDomainService roleAssignmentDomainService;

    @Override
    public List<MembershipView> getMemberships(String userId) {
        return membershipPort.findByUserId(userId).stream()
            .map(membership -> {
                String tenantName = tenantRepository.findById(membership.tenantId())
                    .map(tenant -> tenant.name())
                    .orElse(null);
                return new MembershipView(
                    membership.membershipId(),
                    membership.tenantId(),
                    tenantName,
                    membership.groupType(),
                    membershipProjectionPort.findRoleCodesByMembershipId(membership.membershipId()).stream().sorted().toList(),
                    membership.status(),
                    membershipProjectionPort.findPermissionCodesByMembershipId(membership.membershipId())
                );
            })
            .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public List<MembershipAdminView> listMemberships(ActorContext actor) {
        String tenantId = actor.tenantId();
        return membershipPort.findMembershipsByTenantId(tenantId).stream()
                .map(this::toMembershipView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipDetailResult getMembershipDetail(ActorContext actor, String membershipId) {
        String tenantId = actor.tenantId();
        Membership membership = membershipPort.findMembershipById(membershipId)
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        if (!tenantId.equals(membership.tenantId())) {
            throw new UserAuthDomainException(UserAuthErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }

        UserAccount userAccount = userAccountRepository.findById(membership.userId())
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.USER_NOT_FOUND, "User not found"));

        return new MembershipDetailResult(
                membership.membershipId(),
                membership.tenantId(),
                membership.currentRoleCodes().stream().sorted().toList(),
                membership.status().name(),
                new MembershipDetailResult.UserAccountSummary(
                        userAccount.userId(),
                        userAccount.email().value(),
                        userAccount.phone(),
                        userAccount.status().name(),
                        userAccount.verificationLevel().name()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipRoleAssignmentsResult listMembershipRoleAssignments(ActorContext actor, String membershipId) {
        String tenantId = actor.tenantId();
        Membership target = membershipPort.findMembershipById(membershipId)
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        if (!tenantId.equals(target.tenantId())) {
            throw new UserAuthDomainException(UserAuthErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }
        return new MembershipRoleAssignmentsResult(membershipId, target.currentRoleCodes().stream().sorted().toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipAssignableRolesResult listAssignableRoleCodes(ActorContext actor, String membershipId) {
        String tenantId = actor.tenantId();
        Membership target = membershipPort.findMembershipById(membershipId)
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
        if (!tenantId.equals(target.tenantId())) {
            throw new UserAuthDomainException(UserAuthErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }

        Membership actorMembership = resolveActiveActorMembership(actor.userId(), tenantId);
        Set<String> actorRoles = actorMembership.currentRoleCodes();
        List<String> assignableRoleCodes = membershipProjectionPort.findGlobalEnabledRoleCodes().stream()
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
        List<String> templateCodes = tenantRoleTemplateBindingPort.findTenantRoleTemplateBindings(tenantId).stream()
                .filter(TenantRoleTemplateBindingPort.TenantRoleTemplateBindingView::enabled)
                .map(TenantRoleTemplateBindingPort.TenantRoleTemplateBindingView::templateCode)
                .distinct()
                .sorted()
                .toList();
        return new TenantAvailableTemplateCodesResult(tenantId, templateCodes);
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

    private Membership resolveActiveActorMembership(String actorUserId, String tenantId) {
        return membershipPort.findByUserId(actorUserId).stream()
                .filter(Membership::isActive)
                .filter(membership -> tenantId.equals(membership.tenantId()))
                .findFirst()
                .orElseThrow(() -> new UserAuthDomainException(
                        UserAuthErrorCode.FORBIDDEN_SCOPE,
                        "Actor has no active membership in tenant"));
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
        return userAccountRepository.findById(userId)
                .map(user -> user.email().value())
                .orElse(null);
    }
}

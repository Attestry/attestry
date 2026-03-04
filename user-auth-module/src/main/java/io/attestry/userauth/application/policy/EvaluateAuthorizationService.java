package io.attestry.userauth.application.policy;

import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.port.MembershipPermissionQueryPort;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.authorization.policy.TenantIsolationPolicy;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class EvaluateAuthorizationService implements EvaluateAuthorizationUseCase {

    private final MembershipRepositoryPort membershipRepository;
    private final MembershipPermissionQueryPort membershipPermissionQueryPort;

    public EvaluateAuthorizationService(
        MembershipRepositoryPort membershipRepository,
        MembershipPermissionQueryPort membershipPermissionQueryPort
    ) {
        this.membershipRepository = membershipRepository;
        this.membershipPermissionQueryPort = membershipPermissionQueryPort;
    }

    @Override
    public AuthzEvaluateResult evaluate(ActorContext actor, AuthzEvaluateCommand command) {
        PolicyDecisionMode mode = command.decisionMode() == null
            ? PolicyDecisionMode.TOKEN_SNAPSHOT
            : command.decisionMode();
        Set<String> effectiveScopes = mode == PolicyDecisionMode.LIVE_RECHECK
            ? resolveLiveScopes(actor)
            : actor.scopes();

        if (!effectiveScopes.contains(command.action())) {
            return new AuthzEvaluateResult(false, ErrorCode.FORBIDDEN_SCOPE.name(), effectiveScopes, mode);
        }
        if (!TenantIsolationPolicy.isIsolated(actor.tenantId(), command.tenantId())) {
            return new AuthzEvaluateResult(false, ErrorCode.TENANT_ISOLATION_VIOLATION.name(), effectiveScopes, mode);
        }
        return new AuthzEvaluateResult(true, null, effectiveScopes, mode);
    }

    private Set<String> resolveLiveScopes(ActorContext actor) {
        Set<String> scopes = new LinkedHashSet<>(
            membershipPermissionQueryPort.findPermissionCodesByGlobalRoleCode(RoleCodes.OWNER_DEFAULT)
        );
        if (actor.tenantId() == null) {
            return scopes;
        }
        Membership membership;
        if (actor.groupId() == null) {
            membership = membershipRepository.findByUserId(actor.userId()).stream()
                .filter(Membership::isActive)
                .filter(candidate -> actor.tenantId().equals(candidate.tenantId()))
                .findFirst()
                .orElse(null);
        } else {
            membership = membershipRepository
                .findByUserIdAndContext(actor.userId(), actor.tenantId(), actor.groupId())
                .orElse(null);
        }
        if (membership == null || !membership.isActive()) {
            return scopes;
        }

        scopes.addAll(membershipPermissionQueryPort.findPermissionCodesByMembershipId(membership.membershipId()));
        return scopes;
    }
}

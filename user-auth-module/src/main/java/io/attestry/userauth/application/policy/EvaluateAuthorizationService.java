package io.attestry.userauth.application.policy;

import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.port.MembershipPort;
import io.attestry.userauth.application.port.MembershipProjectionPort;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.authorization.policy.TenantIsolationPolicy;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class EvaluateAuthorizationService implements EvaluateAuthorizationUseCase {

    private final MembershipPort membershipPort;
    private final MembershipProjectionPort membershipProjectionPort;

    public EvaluateAuthorizationService(
        MembershipPort membershipPort,
        MembershipProjectionPort membershipProjectionPort
    ) {
        this.membershipPort = membershipPort;
        this.membershipProjectionPort = membershipProjectionPort;
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
            return new AuthzEvaluateResult(false, UserAuthErrorCode.FORBIDDEN_SCOPE.name(), effectiveScopes, mode);
        }
        if (!TenantIsolationPolicy.isIsolated(actor.tenantId(), command.tenantId())) {
            return new AuthzEvaluateResult(false, UserAuthErrorCode.TENANT_ISOLATION_VIOLATION.name(), effectiveScopes, mode);
        }
        return new AuthzEvaluateResult(true, null, effectiveScopes, mode);
    }

    private Set<String> resolveLiveScopes(ActorContext actor) {
        Set<String> scopes = new LinkedHashSet<>(
            membershipProjectionPort.findPermissionCodesByGlobalRoleCode(RoleCodes.OWNER_DEFAULT)
        );
        if (actor.tenantId() == null) {
            return scopes;
        }
        Membership membership = membershipPort.findByUserIdAndTenantId(actor.userId(), actor.tenantId())
            .orElse(null);
        if (membership == null || !membership.isActive()) {
            return scopes;
        }

        scopes.addAll(membershipProjectionPort.findPermissionCodesByMembershipId(membership.membershipId()));
        return scopes;
    }
}

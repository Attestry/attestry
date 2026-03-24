package io.attestry.userauth.application.policy;

import io.attestry.userauth.application.auth.support.UserEffectiveScopeResolver;
import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.policy.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.policy.command.PolicyDecisionMode;
import io.attestry.userauth.application.policy.result.AuthzEvaluateResult;
import io.attestry.userauth.application.policy.usecase.EvaluateAuthorizationUseCase;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.authorization.policy.TenantIsolationPolicy;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvaluateAuthorizationService implements EvaluateAuthorizationUseCase {

    private final UserEffectiveScopeResolver userEffectiveScopeResolver;

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
        return userEffectiveScopeResolver.resolveEffectiveScopes(actor.userId(), actor.tenantId());
    }
}

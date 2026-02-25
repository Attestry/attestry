package io.attestry.userauth.application.policy;

import io.attestry.userauth.application.dto.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.AuthzEvaluateResult;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.policy.AuthorizationPolicy;
import org.springframework.stereotype.Service;

@Service
public class EvaluateAuthorizationService {

    public AuthzEvaluateResult evaluate(AuthPrincipal principal, AuthzEvaluateCommand command) {
        if (!principal.scopes().contains(command.actionScope())) {
            return new AuthzEvaluateResult(false, ErrorCode.FORBIDDEN_SCOPE.name(), principal.scopes());
        }
        if (!AuthorizationPolicy.isAllowed(principal, command.tenantId(), command.actionScope())) {
            return new AuthzEvaluateResult(false, ErrorCode.TENANT_ISOLATION_VIOLATION.name(), principal.scopes());
        }
        return new AuthzEvaluateResult(true, null, principal.scopes());
    }
}

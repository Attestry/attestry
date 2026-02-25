package io.attestry.userauth.interfaces.policy;

import io.attestry.userauth.application.dto.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.AuthzEvaluateResult;
import io.attestry.userauth.application.policy.EvaluateAuthorizationService;
import io.attestry.userauth.domain.auth.model.Scope;
import io.attestry.userauth.security.AuthPrincipalResolver;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authz")
public class AuthorizationHttp {

    private final EvaluateAuthorizationService evaluateAuthorizationService;

    public AuthorizationHttp(EvaluateAuthorizationService evaluateAuthorizationService) {
        this.evaluateAuthorizationService = evaluateAuthorizationService;
    }

    @PostMapping("/evaluate")
    public AuthzEvaluateResponse evaluate(
        Authentication authentication,
        @RequestBody AuthzEvaluateRequest request
    ) {
        AuthzEvaluateResult result = evaluateAuthorizationService.evaluate(
            AuthPrincipalResolver.resolve(authentication),
            new AuthzEvaluateCommand(request.tenantId(), request.actionScope(), request.resourceRef())
        );

        return new AuthzEvaluateResponse(result.allowed(), result.reason(), result.effectiveScopes());
    }

    public record AuthzEvaluateRequest(String tenantId, Scope actionScope, String resourceRef) {
    }

    public record AuthzEvaluateResponse(boolean allowed, String reason, java.util.Set<Scope> effectiveScopes) {
    }
}

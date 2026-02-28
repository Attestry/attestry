package io.attestry.userauth.interfaces.policy;

import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authz")
public class AuthorizationHttp {

    private final EvaluateAuthorizationUseCase evaluateAuthorizationService;

    public AuthorizationHttp(EvaluateAuthorizationUseCase evaluateAuthorizationService) {
        this.evaluateAuthorizationService = evaluateAuthorizationService;
    }

    @PostMapping("/evaluate")
    public AuthzEvaluateResponse evaluate(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestBody AuthzEvaluateRequest request
    ) {
        AuthzEvaluateResult result = evaluateAuthorizationService.evaluate(
            principal,
            new AuthzEvaluateCommand(request.tenantId(), request.action(), request.resourceRef(), request.decisionMode())
        );

        return new AuthzEvaluateResponse(result.allowed(), result.reason(), result.effectiveScopes(), result.decisionMode());
    }

    public record AuthzEvaluateRequest(String tenantId, String action, String resourceRef, PolicyDecisionMode decisionMode) {
    }

    public record AuthzEvaluateResponse(
        boolean allowed,
        String reason,
        java.util.Set<String> effectiveScopes,
        PolicyDecisionMode decisionMode
    ) {
    }
}

package io.attestry.userauth.interfaces.policy;

import io.attestry.commonlib.web.CurrentActor;
import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.policy.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.policy.command.PolicyDecisionMode;
import io.attestry.userauth.application.policy.result.AuthzEvaluateResult;
import io.attestry.userauth.application.policy.usecase.EvaluateAuthorizationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authz")
@RequiredArgsConstructor
public class AuthorizationHttp {

    private final EvaluateAuthorizationUseCase evaluateAuthorizationService;

    @PostMapping("/evaluate")
    public ApiResponse<AuthzEvaluateResponse> evaluate(
        @CurrentActor ActorContext actor,
        @RequestBody AuthzEvaluateRequest request
    ) {
        AuthzEvaluateResult result = evaluateAuthorizationService.evaluate(
            actor,
            new AuthzEvaluateCommand(request.tenantId(), request.action(), request.resourceRef(), request.decisionMode())
        );

        return ApiResponse.success(new AuthzEvaluateResponse(result.allowed(), result.reason(), result.effectiveScopes(), result.decisionMode()));
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

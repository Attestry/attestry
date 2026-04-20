package io.attestry.userauth.application.contract.authorization;

import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.policy.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.policy.command.PolicyDecisionMode;
import io.attestry.userauth.application.policy.command.EvaluateAuthorizationUseCase;
import io.attestry.userauth.contract.authorization.AuthorizationCheckPort;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationCheckAdapter implements AuthorizationCheckPort {

    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;

    @Override
    public AuthorizationDecision authorize(AuthorizationCheckCommand command) {
        var result = evaluateAuthorizationUseCase.evaluate(
            new ActorContext(
                null,
                command.userId(),
                command.actorTenantId(),
                null,
                command.tokenScopes() == null ? Set.of() : command.tokenScopes(),
                null
            ),
            new AuthzEvaluateCommand(
                command.targetTenantId(),
                command.action(),
                command.resourceRef(),
                toPolicyDecisionMode(command.decisionMode())
            )
        );

        return new AuthorizationDecision(result.allowed(), result.reason());
    }

    private PolicyDecisionMode toPolicyDecisionMode(DecisionMode decisionMode) {
        if (decisionMode == null) {
            return PolicyDecisionMode.TOKEN_SNAPSHOT;
        }
        return switch (decisionMode) {
            case LIVE_RECHECK -> PolicyDecisionMode.LIVE_RECHECK;
            case TOKEN_SNAPSHOT -> PolicyDecisionMode.TOKEN_SNAPSHOT;
        };
    }
}

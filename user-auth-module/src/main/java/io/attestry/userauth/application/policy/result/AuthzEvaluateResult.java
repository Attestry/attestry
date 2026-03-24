package io.attestry.userauth.application.policy.result;

import io.attestry.userauth.application.policy.command.PolicyDecisionMode;
import java.util.Set;

public record AuthzEvaluateResult(
    boolean allowed,
    String reason,
    Set<String> effectiveScopes,
    PolicyDecisionMode decisionMode
) {
}

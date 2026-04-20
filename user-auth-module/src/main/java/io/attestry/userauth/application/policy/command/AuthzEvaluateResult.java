package io.attestry.userauth.application.policy.command;

import java.util.Set;

public record AuthzEvaluateResult(
    boolean allowed,
    String reason,
    Set<String> effectiveScopes,
    PolicyDecisionMode decisionMode
) {
}

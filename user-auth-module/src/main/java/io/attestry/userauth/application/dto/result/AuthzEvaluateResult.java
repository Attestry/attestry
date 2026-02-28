package io.attestry.userauth.application.dto.result;

import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import java.util.Set;

public record AuthzEvaluateResult(
    boolean allowed,
    String reason,
    Set<String> effectiveScopes,
    PolicyDecisionMode decisionMode
) {
}

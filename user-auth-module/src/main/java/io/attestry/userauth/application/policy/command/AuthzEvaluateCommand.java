package io.attestry.userauth.application.policy.command;

public record AuthzEvaluateCommand(
    String tenantId,
    String action,
    String resourceRef,
    PolicyDecisionMode decisionMode
) {
}

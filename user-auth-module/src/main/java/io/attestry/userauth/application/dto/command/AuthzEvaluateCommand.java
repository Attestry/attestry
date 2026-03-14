package io.attestry.userauth.application.dto.command;

public record AuthzEvaluateCommand(
    String tenantId,
    String action,
    String resourceRef,
    PolicyDecisionMode decisionMode
) {
}

package io.attestry.workflow.application.delegation.command;

public record DelegationEvaluateResult(
    boolean allowed,
    String reason
) {
}

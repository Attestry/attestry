package io.attestry.workflow.application.delegation.result;

public record DelegationEvaluateResult(
    boolean allowed,
    String reason
) {
}

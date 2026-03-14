package io.attestry.workflow.interfaces.delegation.dto.response;

public record DelegationEvaluateResponse(boolean allowed, String reason) {
}

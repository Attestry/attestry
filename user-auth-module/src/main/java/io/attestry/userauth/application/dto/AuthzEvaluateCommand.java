package io.attestry.userauth.application.dto;

import io.attestry.userauth.domain.auth.model.Scope;

public record AuthzEvaluateCommand(
    String tenantId,
    Scope actionScope,
    String resourceRef
) {
}

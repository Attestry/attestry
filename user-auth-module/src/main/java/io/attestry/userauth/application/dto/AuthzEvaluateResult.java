package io.attestry.userauth.application.dto;

import io.attestry.userauth.domain.auth.model.Scope;
import java.util.Set;

public record AuthzEvaluateResult(
    boolean allowed,
    String reason,
    Set<Scope> effectiveScopes
) {
}

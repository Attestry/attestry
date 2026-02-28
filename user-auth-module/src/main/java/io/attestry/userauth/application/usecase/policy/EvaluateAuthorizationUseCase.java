package io.attestry.userauth.application.usecase.policy;

import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;

public interface EvaluateAuthorizationUseCase {
    AuthzEvaluateResult evaluate(AuthPrincipal principal, AuthzEvaluateCommand command);
}

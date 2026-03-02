package io.attestry.userauth.application.usecase.policy;

import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;

public interface EvaluateAuthorizationUseCase {
    AuthzEvaluateResult evaluate(ActorContext actor, AuthzEvaluateCommand command);
}

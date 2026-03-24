package io.attestry.userauth.application.policy.usecase;

import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.policy.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.policy.result.AuthzEvaluateResult;

public interface EvaluateAuthorizationUseCase {
    AuthzEvaluateResult evaluate(ActorContext actor, AuthzEvaluateCommand command);
}

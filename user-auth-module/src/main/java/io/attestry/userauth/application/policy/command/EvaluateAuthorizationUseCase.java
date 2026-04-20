package io.attestry.userauth.application.policy.command;

import io.attestry.userauth.application.common.ActorContext;

public interface EvaluateAuthorizationUseCase {
    AuthzEvaluateResult evaluate(ActorContext actor, AuthzEvaluateCommand command);
}

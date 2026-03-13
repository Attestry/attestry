package io.attestry.userauth.application.membership.policy;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.membership.service.RoleAssignmentDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipQueryAccessPolicy {

    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;

    public boolean isRoleAssignableByAuthorization(
        ActorContext actor,
        String tenantId,
        String membershipId,
        RoleAssignmentDomainService.Evaluation roleEvaluation
    ) {
        PolicyDecisionMode mode = roleEvaluation.requiresLiveRecheck()
            ? PolicyDecisionMode.LIVE_RECHECK
            : PolicyDecisionMode.TOKEN_SNAPSHOT;
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            actor,
            new AuthzEvaluateCommand(
                tenantId,
                PermissionCodes.TENANT_ROLE_ASSIGN,
                "membership:" + membershipId + ":role:" + roleEvaluation.normalizedRoleCode(),
                mode
            )
        );
        return decision.allowed();
    }
}

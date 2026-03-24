package io.attestry.userauth.application.membership.policy;

import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.policy.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.policy.command.PolicyDecisionMode;
import io.attestry.userauth.application.policy.result.AuthzEvaluateResult;
import io.attestry.userauth.application.port.membership.MembershipPort;
import io.attestry.userauth.application.policy.usecase.EvaluateAuthorizationUseCase;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.membership.model.Membership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipAccessPolicy {

    private final MembershipPort membershipPort;
    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;

    public void assertLivePermission(ActorContext actor, String tenantId, String action, String resourceRef) {
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            actor,
            new AuthzEvaluateCommand(tenantId, action, resourceRef, PolicyDecisionMode.LIVE_RECHECK)
        );
        if (!decision.allowed()) {
            throw new UserAuthDomainException(
                decision.reason() != null ? UserAuthErrorCode.valueOf(decision.reason()) : UserAuthErrorCode.FORBIDDEN_SCOPE,
                "Action denied by live policy check"
            );
        }
    }

    public Membership loadTenantMembership(String membershipId, String tenantId) {
        Membership membership = membershipPort.findMembershipById(membershipId)
            .orElseThrow(() -> new UserAuthDomainException(
                UserAuthErrorCode.MEMBERSHIP_NOT_FOUND,
                "Membership not found"
            ));
        if (!tenantId.equals(membership.tenantId())) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.TENANT_ISOLATION_VIOLATION,
                "Cross-tenant membership access denied"
            );
        }
        return membership;
    }

    public Membership resolveActiveActorMembership(String actorUserId, String tenantId) {
        return membershipPort.findByUserId(actorUserId).stream()
            .filter(Membership::isActive)
            .filter(membership -> tenantId.equals(membership.tenantId()))
            .findFirst()
            .orElseThrow(() -> new UserAuthDomainException(
                UserAuthErrorCode.FORBIDDEN_SCOPE,
                "Actor has no active membership in tenant"
            ));
    }
}

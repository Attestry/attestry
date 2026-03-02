package io.attestry.userauth.domain.policy;

import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RoleAssignmentDomainService {

    private final RoleAssignmentDecisionPolicy roleAssignmentDecisionPolicy;

    public RoleAssignmentDomainService(RoleAssignmentDecisionPolicy roleAssignmentDecisionPolicy) {
        this.roleAssignmentDecisionPolicy = roleAssignmentDecisionPolicy;
    }

    public Evaluation evaluate(
        Set<String> actorRoleCodes,
        String actorMembershipId,
        String targetMembershipId,
        String requestedRoleCode
    ) {
        String normalizedRoleCode = roleAssignmentDecisionPolicy.normalizeRequestedRoleCode(requestedRoleCode);
        if (normalizedRoleCode == null) {
            return new Evaluation(null, PolicyDecisionMode.TOKEN_SNAPSHOT, DenialReason.INVALID_ROLE_CODE);
        }
        RoleAssignmentDecisionPolicy.Decision decision = roleAssignmentDecisionPolicy.decide(
            actorRoleCodes,
            actorMembershipId,
            targetMembershipId,
            normalizedRoleCode
        );
        PolicyDecisionMode mode = decision.requiresLiveRecheck()
            ? PolicyDecisionMode.LIVE_RECHECK
            : PolicyDecisionMode.TOKEN_SNAPSHOT;
        return new Evaluation(normalizedRoleCode, mode, mapDenialReason(decision.denialReason()));
    }

    private DenialReason mapDenialReason(RoleAssignmentDecisionPolicy.DenialReason reason) {
        return switch (reason) {
            case NONE -> DenialReason.NONE;
            case INVALID_ROLE_CODE -> DenialReason.INVALID_ROLE_CODE;
            case SELF_ESCALATION_DENIED -> DenialReason.SELF_ESCALATION_DENIED;
            case NOT_ASSIGNABLE -> DenialReason.NOT_ASSIGNABLE;
        };
    }

    public enum DenialReason {
        NONE,
        INVALID_ROLE_CODE,
        SELF_ESCALATION_DENIED,
        NOT_ASSIGNABLE
    }

    public record Evaluation(
        String normalizedRoleCode,
        PolicyDecisionMode decisionMode,
        DenialReason denialReason
    ) {
        public boolean denied() {
            return denialReason != DenialReason.NONE;
        }
    }
}

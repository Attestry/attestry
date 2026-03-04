package io.attestry.userauth.domain.membership.service;

import io.attestry.userauth.domain.membership.policy.RoleAssignmentDecisionPolicy;
import java.util.Set;

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
            return new Evaluation(null, false, DenialReason.INVALID_ROLE_CODE);
        }
        RoleAssignmentDecisionPolicy.Decision decision = roleAssignmentDecisionPolicy.decide(
            actorRoleCodes,
            actorMembershipId,
            targetMembershipId,
            normalizedRoleCode
        );
        return new Evaluation(
            normalizedRoleCode,
            decision.requiresLiveRecheck(),
            mapDenialReason(decision.denialReason())
        );
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
        boolean requiresLiveRecheck,
        DenialReason denialReason
    ) {
        public boolean denied() {
            return denialReason != DenialReason.NONE;
        }
    }
}

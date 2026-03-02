package io.attestry.userauth.domain.policy;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RoleAssignmentDecisionPolicy {

    private final RoleAssignmentPolicy roleAssignmentPolicy;

    public RoleAssignmentDecisionPolicy(RoleAssignmentPolicy roleAssignmentPolicy) {
        this.roleAssignmentPolicy = roleAssignmentPolicy;
    }

    public Decision decide(
        Set<String> actorRoleCodes,
        String actorMembershipId,
        String targetMembershipId,
        String requestedRoleCode
    ) {
        String normalizedRoleCode = normalize(requestedRoleCode);
        if (normalizedRoleCode == null) {
            return new Decision(null, false, DenialReason.INVALID_ROLE_CODE);
        }
        if (roleAssignmentPolicy.isSelfEscalationDenied(actorMembershipId, targetMembershipId, normalizedRoleCode)) {
            return new Decision(normalizedRoleCode, roleAssignmentPolicy.requiresLiveRecheck(normalizedRoleCode), DenialReason.SELF_ESCALATION_DENIED);
        }
        if (!roleAssignmentPolicy.canAssign(actorRoleCodes, normalizedRoleCode)) {
            return new Decision(normalizedRoleCode, roleAssignmentPolicy.requiresLiveRecheck(normalizedRoleCode), DenialReason.NOT_ASSIGNABLE);
        }
        return new Decision(normalizedRoleCode, roleAssignmentPolicy.requiresLiveRecheck(normalizedRoleCode), DenialReason.NONE);
    }

    public String normalizeRequestedRoleCode(String roleCode) {
        return normalize(roleCode);
    }

    private String normalize(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }

    public enum DenialReason {
        NONE,
        INVALID_ROLE_CODE,
        SELF_ESCALATION_DENIED,
        NOT_ASSIGNABLE
    }

    public record Decision(
        String normalizedRoleCode,
        boolean requiresLiveRecheck,
        DenialReason denialReason
    ) {
        public boolean denied() {
            return denialReason != DenialReason.NONE;
        }
    }
}

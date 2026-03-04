package io.attestry.userauth.domain.membership.policy;

import java.util.Set;

public interface RoleAssignmentPolicy {

    boolean canAssign(Set<String> actorRoleCodes, String targetRoleCode);

    boolean isSensitiveRole(String roleCode);

    boolean requiresLiveRecheck(String roleCode);

    boolean isSelfEscalationDenied(String actorMembershipId, String targetMembershipId, String roleCode);
}

package io.attestry.userauth.domain.membership.policy;

import io.attestry.userauth.domain.authorization.model.RoleCodes;
import java.util.Set;

public class DefaultRoleAssignmentPolicy implements RoleAssignmentPolicy {

    private static final Set<String> HIGH_PRIVILEGE_ASSIGNABLE = Set.of(
        RoleCodes.TENANT_OWNER
    );

    private static final Set<String> TENANT_OWNER_ASSIGNABLE = Set.of(
        RoleCodes.TENANT_OPERATOR,
        RoleCodes.TENANT_STAFF
    );

    private static final Set<String> DEPRECATED_ASSIGNABLE = Set.of(
        "TENANT_MEMBERSHIP_ADMIN",
        "TENANT_PASSPORT_ADMIN",
        "BRAND_ADMIN_BASE",
        "RETAIL_ADMIN_BASE",
        "BRAND_OPERATOR",
        "RETAIL_OPERATOR",
        "GROUP_STAFF"
    );

    private static final Set<String> SENSITIVE_ROLE_CODES = Set.of(
        RoleCodes.TENANT_OWNER
    );

    @Override
    public boolean canAssign(Set<String> actorRoleCodes, String targetRoleCode) {
        if (actorRoleCodes == null || targetRoleCode == null) {
            return false;
        }
        if (DEPRECATED_ASSIGNABLE.contains(targetRoleCode)) {
            return false;
        }
        if (actorRoleCodes.contains(RoleCodes.PLATFORM_SUPER_ADMIN)) {
            return HIGH_PRIVILEGE_ASSIGNABLE.contains(targetRoleCode) || TENANT_OWNER_ASSIGNABLE.contains(targetRoleCode);
        }
        if (!actorRoleCodes.contains(RoleCodes.TENANT_OWNER)) {
            return false;
        }
        return HIGH_PRIVILEGE_ASSIGNABLE.contains(targetRoleCode) || TENANT_OWNER_ASSIGNABLE.contains(targetRoleCode);
    }

    @Override
    public boolean isSensitiveRole(String roleCode) {
        return roleCode != null && SENSITIVE_ROLE_CODES.contains(roleCode);
    }

    @Override
    public boolean requiresLiveRecheck(String roleCode) {
        return isSensitiveRole(roleCode);
    }

    @Override
    public boolean isSelfEscalationDenied(String actorMembershipId, String targetMembershipId, String roleCode) {
        if (actorMembershipId == null || targetMembershipId == null || roleCode == null) {
            return false;
        }
        return actorMembershipId.equals(targetMembershipId) && isSensitiveRole(roleCode);
    }
}

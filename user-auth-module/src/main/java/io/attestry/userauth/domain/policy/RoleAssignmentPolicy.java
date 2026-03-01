package io.attestry.userauth.domain.policy;

import io.attestry.userauth.domain.auth.model.RoleCodes;
import java.util.Set;

public final class RoleAssignmentPolicy {

    private static final Set<String> PLATFORM_ONLY_ASSIGNABLE = Set.of(
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

    private RoleAssignmentPolicy() {
    }

    public static boolean canAssign(Set<String> actorRoleCodes, String targetRoleCode) {
        if (actorRoleCodes == null || targetRoleCode == null) {
            return false;
        }
        if (DEPRECATED_ASSIGNABLE.contains(targetRoleCode)) {
            return false;
        }
        if (actorRoleCodes.contains(RoleCodes.PLATFORM_SUPER_ADMIN)) {
            return PLATFORM_ONLY_ASSIGNABLE.contains(targetRoleCode) || TENANT_OWNER_ASSIGNABLE.contains(targetRoleCode);
        }
        if (PLATFORM_ONLY_ASSIGNABLE.contains(targetRoleCode)) {
            return false;
        }
        return actorRoleCodes.contains(RoleCodes.TENANT_OWNER)
            && TENANT_OWNER_ASSIGNABLE.contains(targetRoleCode);
    }

    public static boolean isSensitiveRole(String roleCode) {
        return roleCode != null && SENSITIVE_ROLE_CODES.contains(roleCode);
    }

    public static boolean requiresLiveRecheck(String roleCode) {
        return isSensitiveRole(roleCode);
    }

    public static boolean isSelfEscalationDenied(String actorMembershipId, String targetMembershipId, String roleCode) {
        if (actorMembershipId == null || targetMembershipId == null || roleCode == null) {
            return false;
        }
        return actorMembershipId.equals(targetMembershipId) && isSensitiveRole(roleCode);
    }
}

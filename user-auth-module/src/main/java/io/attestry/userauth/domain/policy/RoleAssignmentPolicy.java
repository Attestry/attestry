package io.attestry.userauth.domain.policy;

import io.attestry.userauth.domain.auth.model.RoleCodes;
import java.util.Set;

public final class RoleAssignmentPolicy {

    private static final Set<String> PLATFORM_ONLY_ASSIGNABLE = Set.of(
        RoleCodes.TENANT_OWNER,
        RoleCodes.TENANT_PASSPORT_ADMIN,
        RoleCodes.PLATFORM_SUPER_ADMIN
    );

    private static final Set<String> TENANT_OWNER_ASSIGNABLE = Set.of(
        RoleCodes.BRAND_ADMIN_BASE,
        RoleCodes.RETAIL_ADMIN_BASE,
        RoleCodes.TENANT_MEMBERSHIP_ADMIN
    );

    private static final Set<String> MEMBER_ADMIN_ASSIGNABLE = Set.of(
        RoleCodes.BRAND_OPERATOR,
        RoleCodes.RETAIL_OPERATOR,
        RoleCodes.GROUP_STAFF
    );

    private static final Set<String> SENSITIVE_ROLE_CODES = Set.of(
        RoleCodes.TENANT_OWNER,
        RoleCodes.TENANT_MEMBERSHIP_ADMIN,
        RoleCodes.TENANT_PASSPORT_ADMIN,
        RoleCodes.PLATFORM_SUPER_ADMIN
    );

    private RoleAssignmentPolicy() {
    }

    public static boolean canAssign(Set<String> actorRoleCodes, String targetRoleCode) {
        if (actorRoleCodes == null || targetRoleCode == null) {
            return false;
        }
        if (actorRoleCodes.contains(RoleCodes.PLATFORM_SUPER_ADMIN)) {
            return true;
        }
        if (PLATFORM_ONLY_ASSIGNABLE.contains(targetRoleCode)) {
            return false;
        }
        if (actorRoleCodes.contains(RoleCodes.TENANT_OWNER)
            && (TENANT_OWNER_ASSIGNABLE.contains(targetRoleCode) || MEMBER_ADMIN_ASSIGNABLE.contains(targetRoleCode))) {
            return true;
        }
        return actorRoleCodes.contains(RoleCodes.TENANT_MEMBERSHIP_ADMIN)
            && MEMBER_ADMIN_ASSIGNABLE.contains(targetRoleCode);
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

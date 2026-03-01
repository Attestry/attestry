package io.attestry.userauth.application.port;

import java.util.Set;

public interface MembershipPermissionQueryPort {
    Set<String> findPermissionCodesByMembershipId(String membershipId);

    Set<String> findPermissionCodesByGlobalRoleCode(String roleCode);

    Set<String> findRoleCodesByMembershipId(String membershipId);
}

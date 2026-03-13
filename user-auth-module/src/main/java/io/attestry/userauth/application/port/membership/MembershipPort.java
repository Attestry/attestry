package io.attestry.userauth.application.port.membership;

import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MembershipPort {

    Optional<Membership> findById(String membershipId);

    Membership save(Membership membership);

    List<Membership> findByUserId(String userId);

    Optional<Membership> findByUserIdAndTenantId(String userId, String tenantId);

    List<Membership> findByTenantId(String tenantId);

    void assignRole(String membershipId, String roleCode, String assignedByUserId);

    List<Membership> findMembershipsByTenantId(String tenantId);

    Optional<Membership> findMembershipById(String membershipId);

    Optional<Membership> findMembershipByMembershipIdAndUserId(String membershipId, String userId);

    Membership updateMembership(String tenantId, String membershipId, MembershipRole role, MembershipStatus status);

    void deletePermissionOverrides(String membershipId, Set<String> permissionCodes);

    Set<String> applyPermissionTemplateToMembership(
            String membershipId,
            String templateCode,
            String reason,
            String actorUserId,
            Instant now);

    Set<String> revokePermissionTemplateFromMembership(String membershipId, String templateCode);
}

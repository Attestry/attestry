package io.attestry.userauth.application.port;

import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MembershipAdminRepositoryPort {
    Optional<UserProfileView> findUserById(String userId);

    Invitation saveInvitation(Invitation invitation);

    Optional<Invitation> findInvitationById(String invitationId);

    Membership createMembership(String userId, String tenantId, MembershipRole role);

    List<Membership> findMembershipsByTenantId(String tenantId);

    Optional<Membership> findMembershipById(String membershipId);

    Membership updateMembership(String tenantId, String membershipId, MembershipRole role, MembershipStatus status);

    Set<String> findRoleCodesByMembershipId(String membershipId);

    Set<String> findGlobalEnabledRoleCodes();

    void upsertPermissionOverrides(String membershipId, Set<String> permissionCodes, String source, String reason, String actorUserId, Instant now);

    void deletePermissionOverrides(String membershipId, Set<String> permissionCodes);

    Set<String> applyPermissionTemplateToMembership(String membershipId, String templateCode, String reason, String actorUserId, Instant now);

    Set<String> revokePermissionTemplateFromMembership(String membershipId, String templateCode);

    record UserProfileView(String userId, String email) {
    }
}

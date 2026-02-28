package io.attestry.userauth.application.port;

import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.organization.model.GroupType;
import java.util.List;
import java.util.Optional;

public interface MembershipAdminRepositoryPort {
    Optional<UserProfileView> findUserById(String userId);

    Optional<GroupView> findGroupById(String groupId);

    GroupView saveGroup(GroupView group);

    void updateGroupStatusOnMemberships(String groupId, GroupStatus status);

    Invitation saveInvitation(Invitation invitation);

    Optional<Invitation> findInvitationById(String invitationId);

    Membership createMembership(String userId, String groupId, String tenantId, MembershipRole role);

    List<Membership> findMembershipsByTenantId(String tenantId);

    Optional<Membership> findMembershipById(String membershipId);

    Membership updateMembership(String membershipId, MembershipRole role, MembershipStatus status);

    record UserProfileView(String userId, String email) {
    }

    record GroupView(String groupId, String tenantId, GroupType type, GroupStatus status) {
    }
}

package io.attestry.userauth.domain.membership.repository;

import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.organization.model.GroupStatus;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository {
    Optional<Membership> findById(String membershipId);

    Membership save(Membership membership);

    List<Membership> findByUserId(String userId);

    Optional<Membership> findByUserIdAndContext(String userId, String tenantId, String groupId);

    List<Membership> findByTenantId(String tenantId);

    List<Membership> findByGroupId(String groupId);

    void updateGroupStatusOnMemberships(String groupId, GroupStatus status);
}

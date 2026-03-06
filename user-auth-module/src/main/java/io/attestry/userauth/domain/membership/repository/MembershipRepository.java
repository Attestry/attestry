package io.attestry.userauth.domain.membership.repository;

import io.attestry.userauth.domain.membership.model.Membership;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository {
    Optional<Membership> findById(String membershipId);

    Membership save(Membership membership);

    List<Membership> findByUserId(String userId);

    Optional<Membership> findByUserIdAndTenantId(String userId, String tenantId);

    List<Membership> findByTenantId(String tenantId);
}

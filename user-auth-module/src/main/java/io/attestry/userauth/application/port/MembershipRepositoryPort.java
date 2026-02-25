package io.attestry.userauth.application.port;

import io.attestry.userauth.domain.membership.model.Membership;
import java.util.List;
import java.util.Optional;

public interface MembershipRepositoryPort {
    List<Membership> findByUserId(String userId);

    Optional<Membership> findByUserIdAndContext(String userId, String tenantId, String groupId);
}

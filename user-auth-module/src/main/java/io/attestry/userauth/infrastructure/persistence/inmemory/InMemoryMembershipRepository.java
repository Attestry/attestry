package io.attestry.userauth.infrastructure.persistence.inmemory;

import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.domain.membership.model.Membership;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("inmemory")
public class InMemoryMembershipRepository implements MembershipRepositoryPort {

    private final List<Membership> memberships = new CopyOnWriteArrayList<>();

    @Override
    public List<Membership> findByUserId(String userId) {
        return memberships.stream()
            .filter(membership -> membership.userId().equals(userId))
            .toList();
    }

    @Override
    public Optional<Membership> findByUserIdAndContext(String userId, String tenantId, String groupId) {
        return memberships.stream()
            .filter(membership -> membership.userId().equals(userId))
            .filter(membership -> membership.tenantId().equals(tenantId))
            .filter(membership -> membership.groupId().equals(groupId))
            .findFirst();
    }
}

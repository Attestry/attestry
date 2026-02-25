package io.attestry.userauth.application.membership;

import io.attestry.userauth.application.dto.MembershipView;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.domain.policy.ScopePolicy;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MembershipQueryService {

    private final MembershipRepositoryPort membershipRepository;

    public MembershipQueryService(MembershipRepositoryPort membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public List<MembershipView> getMemberships(String userId) {
        return membershipRepository.findByUserId(userId).stream()
            .map(membership -> new MembershipView(
                membership.membershipId(),
                membership.tenantId(),
                membership.groupId(),
                membership.groupType(),
                membership.role(),
                membership.status(),
                ScopePolicy.forMembership(membership)
            ))
            .toList();
    }
}

package io.attestry.userauth.application.membership;

import io.attestry.userauth.application.dto.view.MembershipView;
import io.attestry.userauth.application.port.MembershipPermissionQueryPort;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.application.usecase.membership.MembershipQueryUseCase;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MembershipQueryService implements MembershipQueryUseCase {

    private final MembershipRepositoryPort membershipRepository;
    private final MembershipPermissionQueryPort membershipPermissionQueryPort;

    public MembershipQueryService(
        MembershipRepositoryPort membershipRepository,
        MembershipPermissionQueryPort membershipPermissionQueryPort
    ) {
        this.membershipRepository = membershipRepository;
        this.membershipPermissionQueryPort = membershipPermissionQueryPort;
    }

    @Override
    public List<MembershipView> getMemberships(String userId) {
        return membershipRepository.findByUserId(userId).stream()
            .map(membership -> new MembershipView(
                membership.membershipId(),
                membership.tenantId(),
                membership.groupId(),
                membership.groupType(),
                membership.role(),
                membership.status(),
                membershipPermissionQueryPort.findPermissionCodesByMembershipId(membership.membershipId())
            ))
            .toList();
    }
}

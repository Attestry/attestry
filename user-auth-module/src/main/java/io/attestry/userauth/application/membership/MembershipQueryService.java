package io.attestry.userauth.application.membership;

import io.attestry.userauth.application.dto.view.MembershipView;
import io.attestry.userauth.application.port.MembershipPermissionQueryPort;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.application.usecase.membership.MembershipQueryUseCase;
import io.attestry.userauth.domain.organization.repository.TenantRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MembershipQueryService implements MembershipQueryUseCase {

    private final MembershipRepositoryPort membershipRepository;
    private final MembershipPermissionQueryPort membershipPermissionQueryPort;
    private final TenantRepository tenantRepository;

    public MembershipQueryService(
        MembershipRepositoryPort membershipRepository,
        MembershipPermissionQueryPort membershipPermissionQueryPort,
        TenantRepository tenantRepository
    ) {
        this.membershipRepository = membershipRepository;
        this.membershipPermissionQueryPort = membershipPermissionQueryPort;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public List<MembershipView> getMemberships(String userId) {
        return membershipRepository.findByUserId(userId).stream()
            .map(membership -> {
                String tenantName = tenantRepository.findById(membership.tenantId())
                    .map(tenant -> tenant.name())
                    .orElse(null);
                return new MembershipView(
                    membership.membershipId(),
                    membership.tenantId(),
                    tenantName,
                    membership.groupType(),
                    membershipPermissionQueryPort.findRoleCodesByMembershipId(membership.membershipId()).stream().sorted().toList(),
                    membership.status(),
                    membershipPermissionQueryPort.findPermissionCodesByMembershipId(membership.membershipId())
                );
            })
            .toList();
    }
}

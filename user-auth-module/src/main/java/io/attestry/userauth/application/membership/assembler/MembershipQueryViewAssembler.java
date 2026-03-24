package io.attestry.userauth.application.membership.assembler;

import io.attestry.userauth.application.membership.view.MembershipAdminView;
import io.attestry.userauth.application.membership.view.MembershipDetailView;
import io.attestry.userauth.application.membership.view.MembershipView;
import io.attestry.userauth.application.port.membership.MembershipProjectionPort;
import io.attestry.userauth.application.port.tenant.TenantRepositoryPort;
import io.attestry.userauth.application.port.identity.UserAccountRepositoryPort;
import io.attestry.userauth.domain.auth.model.UserAccount;
import io.attestry.userauth.domain.membership.model.Membership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipQueryViewAssembler {

    private final MembershipProjectionPort membershipProjectionPort;
    private final TenantRepositoryPort tenantRepository;
    private final UserAccountRepositoryPort userAccountRepository;

    public MembershipView toMembershipView(Membership membership) {
        String tenantName = tenantRepository.findById(membership.tenantId())
            .map(tenant -> tenant.name())
            .orElse(null);
        return new MembershipView(
            membership.membershipId(),
            membership.tenantId(),
            tenantName,
            membership.groupType(),
            membershipProjectionPort.findRoleCodesByMembershipId(membership.membershipId()).stream().sorted().toList(),
            membership.status(),
            membershipProjectionPort.findPermissionCodesByMembershipId(membership.membershipId())
        );
    }

    public MembershipAdminView toAdminView(Membership membership) {
        return new MembershipAdminView(
            membership.membershipId(),
            membership.tenantId(),
            resolveUserEmail(membership.userId()),
            membership.currentRoleCodes().stream().sorted().toList(),
            membership.status().name()
        );
    }

    public MembershipDetailView toDetailResult(Membership membership, UserAccount userAccount) {
        return new MembershipDetailView(
            membership.membershipId(),
            membership.tenantId(),
            membership.currentRoleCodes().stream().sorted().toList(),
            membership.status().name(),
            new MembershipDetailView.UserAccountSummary(
                userAccount.userId(),
                userAccount.email().value(),
                userAccount.phone(),
                userAccount.status().name(),
                userAccount.verificationLevel().name()
            )
        );
    }

    private String resolveUserEmail(String userId) {
        return userAccountRepository.findById(userId)
            .map(user -> user.email().value())
            .orElse(null);
    }
}

package io.attestry.userauth.application.membership;

import io.attestry.userauth.application.port.MembershipAdminRepositoryPort;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.auth.model.RoleCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import org.springframework.stereotype.Component;

@Component
public class PlatformSuperAdminAuthorizationChecker {

    private final MembershipRepositoryPort membershipRepository;
    private final MembershipAdminRepositoryPort membershipAdminRepository;

    public PlatformSuperAdminAuthorizationChecker(
        MembershipRepositoryPort membershipRepository,
        MembershipAdminRepositoryPort membershipAdminRepository
    ) {
        this.membershipRepository = membershipRepository;
        this.membershipAdminRepository = membershipAdminRepository;
    }

    public void assertPlatformSuperAdmin(String userId) {
        boolean allowed = membershipRepository.findByUserId(userId).stream()
            .map(Membership::membershipId)
            .map(membershipAdminRepository::findRoleCodesByMembershipId)
            .anyMatch(roleCodes -> roleCodes.contains(RoleCodes.PLATFORM_SUPER_ADMIN));
        if (!allowed) {
            throw new DomainException(ErrorCode.FORBIDDEN_SCOPE, "PLATFORM_SUPER_ADMIN role is required");
        }
    }
}

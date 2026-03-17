package io.attestry.userauth.application.membership.policy;

import io.attestry.userauth.application.port.membership.MembershipPort;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveOwnerGuard {

    private final MembershipPort membershipPort;

    public void assertCanUpdateStatus(Membership target, MembershipStatus newStatus) {
        if (newStatus != MembershipStatus.SUSPENDED || !isActiveOwner(target)) {
            return;
        }
        assertActiveOwnerRemains(target.tenantId());
    }

    public void assertCanRevokeRole(Membership target, String roleCode) {
        if (!RoleCodes.TENANT_OWNER.equalsIgnoreCase(roleCode) || !isActiveOwner(target)) {
            return;
        }
        assertActiveOwnerRemains(target.tenantId());
    }

    private void assertActiveOwnerRemains(String tenantId) {
        long activeOwnerCount = membershipPort.findMembershipsByTenantId(tenantId).stream()
            .filter(this::isActiveOwner)
            .count();
        if (activeOwnerCount <= 1) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.LAST_ACTIVE_OWNER_REQUIRED,
                "시스템에 최소 한 명의 관리자가 필요합니다."
            );
        }
    }

    private boolean isActiveOwner(Membership membership) {
        return membership.isActive() && membership.currentRoleCodes().contains(RoleCodes.TENANT_OWNER);
    }
}

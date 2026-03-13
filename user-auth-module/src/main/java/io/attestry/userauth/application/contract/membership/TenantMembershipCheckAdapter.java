package io.attestry.userauth.application.contract.membership;

import io.attestry.userauth.application.port.membership.MembershipPort;
import io.attestry.userauth.contract.membership.TenantMembershipCheckPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantMembershipCheckAdapter implements TenantMembershipCheckPort {

    private final MembershipPort membershipPort;

    @Override
    public MembershipCheckResult checkActiveMembership(String userId, String tenantId, String tenantType) {
        return membershipPort.findByUserIdAndTenantId(userId, tenantId)
            .map(membership -> new MembershipCheckResult(
                membership.isActive() && membership.groupType().name().equals(tenantType),
                membership.tenantId(),
                membership.groupType().name()
            ))
            .orElseGet(() -> new MembershipCheckResult(false, tenantId, tenantType));
    }
}

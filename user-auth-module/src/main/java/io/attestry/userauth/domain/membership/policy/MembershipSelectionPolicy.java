package io.attestry.userauth.domain.membership.policy;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.membership.model.Membership;
import java.util.List;
import java.util.Optional;

public final class MembershipSelectionPolicy {

    private MembershipSelectionPolicy() {
    }

    public static Membership resolve(
        String requestedTenantId,
        String requestedGroupId,
        Optional<Membership> requestedMembership,
        List<Membership> memberships
    ) {
        if (requestedTenantId != null && requestedGroupId != null) {
            Membership membership = requestedMembership
                .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
            if (!membership.isActive()) {
                throw new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership inactive");
            }
            return membership;
        }
        return memberships.stream().filter(Membership::isActive).findFirst().orElse(null);
    }
}

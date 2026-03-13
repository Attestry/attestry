package io.attestry.userauth.domain.membership.policy;

import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;import io.attestry.userauth.domain.membership.model.Membership;
import java.util.List;
import java.util.Optional;

public final class MembershipSelectionPolicy {

    private MembershipSelectionPolicy() {
    }

    public static Optional<Membership> resolve(
        String requestedTenantId,
        Optional<Membership> requestedMembership,
        List<Membership> memberships
    ) {
        if (requestedTenantId != null) {
            Membership membership = requestedMembership
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
            if (!membership.isActive()) {
                throw new UserAuthDomainException(UserAuthErrorCode.MEMBERSHIP_NOT_FOUND, "Membership inactive");
            }
            return Optional.of(membership);
        }
        return memberships.stream().filter(Membership::isActive).findFirst();
    }
}

package io.attestry.userauth.domain.membership.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.tenant.model.TenantType;
import io.attestry.userauth.domain.tenant.model.TenantStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MembershipSelectionPolicyTest {

    @Test
    void shouldReturnRequestedMembershipWhenActive() {
        Membership requested = membership("m1", MembershipStatus.ACTIVE, TenantStatus.ACTIVE);

        Membership resolved = MembershipSelectionPolicy.resolve(
            "t1",
            Optional.of(requested),
            List.of(requested)
        ).orElseThrow();

        assertEquals("m1", resolved.membershipId());
    }

    @Test
    void shouldFailWhenRequestedMembershipMissing() {
        UserAuthDomainException ex = assertThrows(UserAuthDomainException.class, () ->
            MembershipSelectionPolicy.resolve("t1", Optional.empty(), List.of()));

        assertEquals(UserAuthErrorCode.MEMBERSHIP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldFailWhenRequestedMembershipInactive() {
        Membership inactive = membership("m1", MembershipStatus.SUSPENDED, TenantStatus.ACTIVE);

        UserAuthDomainException ex = assertThrows(UserAuthDomainException.class, () ->
            MembershipSelectionPolicy.resolve("t1", Optional.of(inactive), List.of(inactive)));

        assertEquals(UserAuthErrorCode.MEMBERSHIP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldReturnFirstActiveMembershipWhenNoContextRequested() {
        Membership inactive = membership("m1", MembershipStatus.SUSPENDED, TenantStatus.ACTIVE);
        Membership active = membership("m2", MembershipStatus.ACTIVE, TenantStatus.ACTIVE);

        Membership resolved = MembershipSelectionPolicy.resolve(
            null,
            Optional.empty(),
            List.of(inactive, active)
        ).orElseThrow();

        assertEquals("m2", resolved.membershipId());
    }

    @Test
    void shouldReturnNullWhenNoActiveMembershipAndNoContextRequested() {
        Membership inactive = membership("m1", MembershipStatus.SUSPENDED, TenantStatus.ACTIVE);

        Optional<Membership> resolved = MembershipSelectionPolicy.resolve(
            null,
            Optional.empty(),
            List.of(inactive)
        );

        assertEquals(Optional.empty(), resolved);
    }

    private Membership membership(String id, MembershipStatus status, TenantStatus tenantStatus) {
        return Membership.reconstitute(
            id, "u1", "t1",
            TenantType.BRAND, MembershipRole.ADMIN, status,
            tenantStatus, Set.of()
        );
    }
}

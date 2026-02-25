package io.attestry.userauth.domain.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.auth.model.Scope;
import io.attestry.userauth.domain.user.enums.VerificationLevel;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthorizationPolicyTest {

    @Test
    void shouldAllowWhenScopeAndTenantMatch() {
        AuthPrincipal principal = new AuthPrincipal(
            "t1",
            "u1",
            "tenant-a",
            "group-a",
            VerificationLevel.NONE,
            Set.of(Scope.BRAND_MINT),
            Instant.parse("2026-02-25T01:00:00Z")
        );

        assertTrue(AuthorizationPolicy.isAllowed(principal, "tenant-a", Scope.BRAND_MINT));
    }

    @Test
    void shouldDenyWhenScopeMissing() {
        AuthPrincipal principal = new AuthPrincipal(
            "t1",
            "u1",
            "tenant-a",
            "group-a",
            VerificationLevel.NONE,
            Set.of(Scope.OWNER_TRANSFER_CREATE),
            Instant.parse("2026-02-25T01:00:00Z")
        );

        assertFalse(AuthorizationPolicy.isAllowed(principal, "tenant-a", Scope.BRAND_MINT));
    }

    @Test
    void shouldDenyWhenTenantDifferent() {
        AuthPrincipal principal = new AuthPrincipal(
            "t1",
            "u1",
            "tenant-a",
            "group-a",
            VerificationLevel.NONE,
            Set.of(Scope.BRAND_MINT),
            Instant.parse("2026-02-25T01:00:00Z")
        );

        assertFalse(AuthorizationPolicy.isAllowed(principal, "tenant-b", Scope.BRAND_MINT));
    }
}

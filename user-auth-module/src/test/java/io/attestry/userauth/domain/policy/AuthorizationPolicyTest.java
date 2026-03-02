package io.attestry.userauth.domain.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.domain.auth.model.PermissionCodes;
import io.attestry.userauth.domain.user.enums.VerificationLevel;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthorizationPolicyTest {

    @Test
    void shouldAllowWhenScopeAndTenantMatch() {
        ActorContext actor = new ActorContext(
            "t1",
            "u1",
            "tenant-a",
            "group-a",
            VerificationLevel.NONE,
            Set.of(PermissionCodes.BRAND_MINT),
            Instant.parse("2026-02-25T01:00:00Z")
        );

        assertTrue(AuthorizationPolicy.isAllowed(actor, "tenant-a", PermissionCodes.BRAND_MINT));
    }

    @Test
    void shouldDenyWhenScopeMissing() {
        ActorContext actor = new ActorContext(
            "t1",
            "u1",
            "tenant-a",
            "group-a",
            VerificationLevel.NONE,
            Set.of(PermissionCodes.OWNER_TRANSFER_CREATE),
            Instant.parse("2026-02-25T01:00:00Z")
        );

        assertFalse(AuthorizationPolicy.isAllowed(actor, "tenant-a", PermissionCodes.BRAND_MINT));
    }

    @Test
    void shouldDenyWhenTenantDifferent() {
        ActorContext actor = new ActorContext(
            "t1",
            "u1",
            "tenant-a",
            "group-a",
            VerificationLevel.NONE,
            Set.of(PermissionCodes.BRAND_MINT),
            Instant.parse("2026-02-25T01:00:00Z")
        );

        assertFalse(AuthorizationPolicy.isAllowed(actor, "tenant-b", PermissionCodes.BRAND_MINT));
    }
}
